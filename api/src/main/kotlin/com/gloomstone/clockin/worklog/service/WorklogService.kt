package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.worklog.domain.*
import com.gloomstone.clockin.worklog.dto.TimeEntryResponse
import com.gloomstone.clockin.worklog.dto.UpdateWorkdayRequest
import com.gloomstone.clockin.worklog.repository.DayRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import com.gloomstone.clockin.worklog.util.toDuration
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*

/**
 * Service class for handling worklog related operations.
 */
@Service
class WorklogService(
    private val timeEntryRepository: TimeEntryRepository,
    private val dayRepository: DayRepository,
    private val snapshotService: SnapshotService,
    private val settingService: SettingService,
    private val eventService: EventService,
    private val holidayService: HolidayService,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(WorklogService::class.java)
    private val calculator = WorklogCalculator()

    /** Records the next check-in or check-out for the local worklog. */
    @Transactional
    fun recordEntry(): Status {
        val now = LocalDateTime.now(clock)
        val date = now.toLocalDate()
        logger.info("Recording clock action, date={} timestamp={}", date, now)

        snapshotService.invalidateFrom(date)
        val day = dayRepository.findByDate(date) ?: createDay(date)

        if (day.entries.isEmpty() || day.entries.last().end != null) {
            val timeEntry = TimeEntry(now, day)
            addEntry(day, timeEntry)
            logger.info("Checked in, date={} entryId={} start={}", date, timeEntry.id, now)
        } else {
            val last = day.entries.last()
            last.end = now
            timeEntryRepository.save(last)
            logger.info(
                "Checked out, date={} entryId={} start={} end={} duration={}",
                date,
                last.id,
                last.start,
                last.end,
                last.start?.let { Duration.between(it, now) },
            )
        }
        return calcStatus(day).also { status ->
            logger.info(
                "Clock action completed, date={} checkedIn={} gross={} balance={}",
                date,
                status.isCheckedIn,
                status.gross,
                status.balance,
            )
        }
    }

    @Transactional
    fun update(request: UpdateWorkdayRequest): Workday {
        val date = request.date
        logger.info("Updating workday, date={} requestedEntries={}", date, request.entries.size)
        val validatedEntries = request.entries.map { validateEntry(it, date) }
        val correctionCount = validatedEntries.count { it is ValidatedEntry.Correction }
        if (correctionCount > 1) {
            rejectUpdate(date, "Only one correction entry is allowed per day")
        }
        snapshotService.invalidateFrom(date)
        val day = dayRepository.findByDate(date) ?: createDay(date)
        val replacedEntryCount = day.entries.size

        timeEntryRepository.deleteAll(day.entries)

        val newLogEntries = validatedEntries.mapTo(mutableListOf()) { it.toEntity(day) }
        val sortedEntries = getSortedList(newLogEntries)
        timeEntryRepository.saveAll(sortedEntries)

        day.entries = newLogEntries

        calcBalance()

        return dayRepository.save(day).also { savedDay ->
            logger.info(
                "Workday updated, date={} replacedEntries={} savedEntries={} standardEntries={} correctionEntries={} gross={} balance={}",
                date,
                replacedEntryCount,
                savedDay.entries.size,
                validatedEntries.size - correctionCount,
                correctionCount,
                savedDay.gross,
                savedDay.balance,
            )
        }
    }

    private fun validateEntry(entry: TimeEntryResponse, date: LocalDate): ValidatedEntry =
        when (entry.type) {
            EntryType.STANDARD.value -> {
                val start = entry.start
                    ?: rejectUpdate(date, "Standard entries require a start time", entry.type)
                val end = entry.end

                if (start.toLocalDate() != date || end?.toLocalDate()?.let { it != date } == true) {
                    rejectUpdate(date, "Entry timestamps must be on $date", entry.type)
                }
                if (end?.isBefore(start) == true) {
                    rejectUpdate(date, "Entry end time must not be before its start time", entry.type)
                }

                ValidatedEntry.Standard(start, end)
            }

            EntryType.CORRECTION.value -> {
                val duration = entry.duration
                    ?: rejectUpdate(date, "Correction entries require a duration", entry.type)
                if (duration.toDuration() == null) {
                    rejectUpdate(date, "Invalid correction duration: $duration", entry.type)
                }
                ValidatedEntry.Correction(duration)
            }

            else -> rejectUpdate(date, "Unknown entry type: ${entry.type}", entry.type)
        }

    private fun rejectUpdate(date: LocalDate, reason: String, entryType: String? = null): Nothing {
        logger.info("Workday update rejected, date={} entryType={} reason={}", date, entryType, reason)
        throw BadRequestException(reason)
    }

    @Transactional
    fun createDay(date: LocalDate): Workday {
        val workday = Workday(date)
        return dayRepository.save(workday).also {
            logger.info("Created workday, date={} workdayId={}", date, it.id)
        }
    }

    @Transactional
    fun addEntry(workday: Workday, timeEntry: TimeEntry) {
        timeEntry.workday = workday
        workday.entries.add(timeEntry)

        timeEntryRepository.save(timeEntry)
        dayRepository.save(workday)
        logger.info(
            "Added entry to workday, date={} workdayId={} entryId={} type={} start={} end={} duration={}",
            workday.date,
            workday.id,
            timeEntry.id,
            timeEntry.type,
            timeEntry.start,
            timeEntry.end,
            timeEntry.duration,
        )
    }

    @Transactional
    fun getBalance(): Status {
        val now = LocalDate.now(clock)
        val workday = dayRepository.findByDate(now) ?: Workday(now)
        return calcStatus(workday).also { status ->
            logger.info(
                "Current status loaded, date={} checkedIn={} gross={} balance={}",
                now,
                status.isCheckedIn,
                status.gross,
                status.balance,
            )
        }
    }

    @Transactional
    fun getEntries(month: LocalDate): List<Workday> {
        val start = month.withDayOfMonth(1)
        val end = month.with(TemporalAdjusters.lastDayOfMonth())
        logger.info("Loading monthly worklog, month={} rangeStart={} rangeEnd={}", month.month, start, end)

        holidayService.ensureYearLoaded(month.year)
        calcBalance()

        val days = dayRepository.findAllByDateGreaterThanEqualAndDateLessThanEqualOrderByDate(start, end)

        val map: MutableMap<String, Workday?> = TreeMap()
        val dist = Period.between(start, end).days
        val s = start.dayOfMonth
        (s..s + dist).forEach { i ->
            val date = start.withDayOfMonth(i)
            val key = date.format(DateTimeFormatter.ofPattern(DATE_FORMAT))
            map[key] = null
        }
        days.forEach { i: Workday ->
            if (i.date.month == month.month) {
                i.isWorkday = isWorkday(i)
                val key = i.date.format(DateTimeFormatter.ofPattern(DATE_FORMAT))
                map[key] = i
            }
        }
        setEmptyDays(map)
        return ArrayList(map.values.filterNotNull()).also { result ->
            logger.info(
                "Monthly worklog loaded, month={} persistedDays={} returnedDays={}",
                month.month,
                days.size,
                result.size,
            )
        }
    }

    @Transactional
    fun getEntries(typeStr: String?): List<TimeEntry> {
        val type = if (typeStr == "correction") {
            EntryType.CORRECTION
        } else {
            EntryType.STANDARD
        }

        return timeEntryRepository.findAllByType(type).also { entries ->
            logger.info("Time entries loaded, requestedType={} resolvedType={} count={}", typeStr, type, entries.size)
        }
    }

    private fun getSortedList(newLogEntries: List<TimeEntry>): List<TimeEntry> {
        return newLogEntries.sortedWith(compareBy<TimeEntry> { it.start == null }.thenBy { it.start })
    }


    private fun setEmptyDays(map: MutableMap<String, Workday?>) {
        map.forEach { (key: String, value: Workday?) ->
            if (value == null) {
                val date = LocalDate.parse(key, DateTimeFormatter.ofPattern(DATE_FORMAT))
                val d = Workday(date)
                d.isWorkday = isWorkday(d)
                map[key] = d
            }
        }
    }

    @Transactional
    fun delete(id: Long) {
        val entry = timeEntryRepository.findById(id).orElseThrow()
        timeEntryRepository.delete(entry)

        snapshotService.delete()
        logger.info(
            "Deleted time entry and reset balance snapshot, entryId={} date={} type={}",
            id,
            entry.workday?.date,
            entry.type,
        )
    }

    @Transactional
    fun deleteAll() {
        timeEntryRepository.deleteAll()
        dayRepository.deleteAll()
        snapshotService.delete()
        logger.info("Deleted all time entries and workdays and reset balance snapshot")
    }


    /** Calculates the current check-in status and totals for a workday. */
    @Transactional
    fun calcStatus(workday: Workday): Status {
        if (workday.entries.isEmpty()) {
            logger.info("Calculated empty workday status, date={}", workday.date)
            return Status(
                false,
                Duration.ZERO,
                Duration.ZERO
            )
        }
        val foundDay = (calcBalance().find { it.date == workday.date } ?: workday)
        val checkedIn = isCheckedIn(foundDay)

        return Status(
            checkedIn,
            foundDay.gross ?: Duration.ZERO,
            foundDay.balance ?: Duration.ZERO,
        ).also { status ->
            logger.info(
                "Calculated workday status, date={} entries={} checkedIn={} gross={} balance={}",
                foundDay.date,
                foundDay.entries.size,
                status.isCheckedIn,
                status.gross,
                status.balance,
            )
        }
    }

    /** Loads the worklog, applies deterministic calculations, and persists the resulting totals. */
    @Transactional
    fun calcBalance(): List<Workday> {
        val settings = settingService.get()
        val now = LocalDateTime.now(clock)

        val firstRelevantYear = dayRepository.findFirstByOrderByDateAsc()?.date?.year ?: now.year
        (firstRelevantYear..now.year).forEach(holidayService::ensureYearLoaded)

        val snapshot = snapshotService.get()

        val snapshotWorkday = snapshot.workday
        logger.info(
            "Starting balance recalculation, now={} snapshotDate={} snapshotBalance={} workingHours={} breakTime={} workingDaysMask={}",
            now,
            snapshotWorkday?.date,
            snapshotWorkday?.balance,
            settings.workingHours,
            settings.breakTime,
            settings.workingDays,
        )
        val workdays: List<Workday> = if (snapshotWorkday == null) {
            dayRepository.findAllByOrderByDate()
        } else {
            dayRepository.findAllByDateGreaterThanEqualOrderByDate(snapshotWorkday.date)
        }
        logger.info(
            "Loaded workdays for balance recalculation, mode={} count={} firstDate={} lastDate={}",
            if (snapshotWorkday == null) "full" else "from-snapshot",
            workdays.size,
            workdays.firstOrNull()?.date,
            workdays.lastOrNull()?.date,
        )

        val localDateDayMap = calcDays(workdays)

        for (d in workdays) {
            val key = d.date
            localDateDayMap[key] = d
        }

        var previousBalance: Duration? = null

        val snapshotDuration = snapshotWorkday?.balance ?: Duration.ZERO

        for (entry in localDateDayMap.entries) {
            var value = entry.value
            if (value == null) {
                value = createDay(entry.key)

                localDateDayMap[entry.key] = value
            }
            val workday = isWorkday(value, settings.workingDays)
            val totals = calculator.calculate(
                workday = value,
                previousBalance = previousBalance,
                workingHours = settings.workingHours,
                breakTime = settings.breakTime,
                now = now,
                isWorkday = workday,
                snapshotBalance = snapshotDuration.takeIf { value.date == snapshotWorkday?.date },
            )

            value.isWorkday = workday
            value.gross = totals.gross
            value.balance = totals.balance
            previousBalance = totals.balance
            logger.info(
                "Calculated day, date={} entries={} isWorkday={} snapshotApplied={} gross={} balance={}",
                value.date,
                value.entries.size,
                workday,
                value.date == snapshotWorkday?.date,
                totals.gross,
                totals.balance,
            )
        }
        dayRepository.saveAll(workdays)

        val calculatedDays = localDateDayMap.values.filterNotNull()
        calculatedDays.lastOrNull()?.let(snapshotService::advanceTo)

        logger.info(
            "Balance recalculation completed, calculatedDays={} persistedDays={} snapshotAdvancedTo={} finalBalance={}",
            calculatedDays.size,
            workdays.size,
            calculatedDays.lastOrNull()?.date,
            calculatedDays.lastOrNull()?.balance,
        )

        return ArrayList(calculatedDays)
    }

    private fun calcDays(workdays: List<Workday>): MutableMap<LocalDate, Workday?> {
        val now = LocalDate.now(clock)
        val map = TreeMap<LocalDate, Workday?>()
        val firstDay = workdays.firstOrNull()?.date ?: LocalDate.now(clock)
        val duration = ChronoUnit.DAYS.between(firstDay, now)
        var tmp = firstDay
        for (i in 0..duration) {
            map[tmp] = null
            tmp = tmp.plusDays(1)
        }
        return map
    }

    private fun isWorkday(
        workday: Workday,
        workingDays: Long = settingService.get().workingDays,
    ): Boolean {
        // Mo Di Mi Do Fr Sa So
        // 1  2  4  8  16 32 64
        val dayFlag = 1L shl (workday.date.dayOfWeek.value - 1)
        val workDay = workingDays and dayFlag != 0L
        if (!workDay) {
            logger.info("Classified non-workday, date={} reason=working-day-schedule", workday.date)
            return false
        }
        val event = eventService.findByDate(workday.date)
        if (event.isNotEmpty()) {
            val reason = if (event.any { it.type == EventType.HOLIDAY }) "holiday" else "event"
            logger.info("Classified non-workday, date={} reason={} eventCount={}", workday.date, reason, event.size)
            return false
        }
        logger.info("Classified workday, date={}", workday.date)
        return true
    }

    private fun isCheckedIn(workday: Workday): Boolean {
        return calculator.isCheckedIn(workday.entries)
    }

    companion object {
        const val DATE_FORMAT = "yyyy-MM-dd"
    }
}

private sealed interface ValidatedEntry {
    fun toEntity(workday: Workday): TimeEntry

    data class Standard(
        val start: LocalDateTime,
        val end: LocalDateTime?,
    ) : ValidatedEntry {
        override fun toEntity(workday: Workday): TimeEntry =
            end?.let { TimeEntry(start, it, workday) } ?: TimeEntry(start, workday)
    }

    data class Correction(val duration: String) : ValidatedEntry {
        override fun toEntity(workday: Workday) = TimeEntry(duration, EntryType.CORRECTION, workday)
    }
}
