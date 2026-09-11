package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.worklog.domain.EntryType
import com.gloomstone.clockin.worklog.domain.Holiday
import com.gloomstone.clockin.worklog.domain.Status
import com.gloomstone.clockin.worklog.domain.TimeEntry
import com.gloomstone.clockin.worklog.domain.Workday
import com.gloomstone.clockin.worklog.dto.TimeEntryResponse
import com.gloomstone.clockin.worklog.dto.UpdateWorkdayRequest
import com.gloomstone.clockin.worklog.repository.DayRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import com.gloomstone.clockin.worklog.util.toDuration
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.ArrayList
import java.util.TreeMap

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
        logger.info("time entry recorded")

        val day = dayRepository.findByDate(now.toLocalDate()) ?: createDay(now.toLocalDate())

        if (day.entries.isEmpty() || day.entries.last().end != null) {
            val timeEntry = TimeEntry(now, day)
            addEntry(day, timeEntry)
        } else {
            val last = day.entries.last()
            last.end = now
            logger.info("write booking, booking={}", last.id)
            timeEntryRepository.save(last)
        }
        logger.info("time entry recorded")
        return calcStatus(day)
    }

    @Transactional
    fun update(request: UpdateWorkdayRequest): Workday {
        val date = request.date
        val validatedEntries = request.entries.map { validateEntry(it, date) }
        val day = dayRepository.findByDate(date) ?: createDay(date)

        timeEntryRepository.deleteAll(day.entries)

        val newLogEntries = validatedEntries.mapTo(mutableListOf()) { it.toEntity(day) }
        val sortedEntries = getSortedList(newLogEntries)
        timeEntryRepository.saveAll(sortedEntries)

        day.entries = newLogEntries

        snapshotService.createSnapshot(date)
        calcBalance()

        return dayRepository.save(day).also {
            logger.info("day updated, day={}", day.date)
        }
    }

    private fun validateEntry(entry: TimeEntryResponse, date: LocalDate): ValidatedEntry =
        when (entry.type) {
            EntryType.STANDARD.value -> {
                val start = entry.start
                    ?: throw BadRequestException("Standard entries require a start time")
                val end = entry.end

                if (start.toLocalDate() != date || end?.toLocalDate()?.let { it != date } == true) {
                    throw BadRequestException("Entry timestamps must be on $date")
                }
                if (end?.isBefore(start) == true) {
                    throw BadRequestException("Entry end time must not be before its start time")
                }

                ValidatedEntry.Standard(start, end)
            }

            EntryType.CORRECTION.value -> {
                val duration = entry.duration
                    ?: throw BadRequestException("Correction entries require a duration")
                if (duration.toDuration() == null) {
                    throw BadRequestException("Invalid correction duration: $duration")
                }
                ValidatedEntry.Correction(duration)
            }

            else -> throw BadRequestException("Unknown entry type: ${entry.type}")
        }

    private fun getSortedList(newLogEntries: List<TimeEntry>): List<TimeEntry> =
        newLogEntries.sortedWith(compareBy<TimeEntry> { it.start == null }.thenBy { it.start })

    @Transactional
    fun createDay(date: LocalDate): Workday {
        val workday = Workday(date)
        return dayRepository.save(workday).also {
            logger.info("day created, date={}", date)
        }
    }

    @Transactional
    fun addEntry(workday: Workday, timeEntry: TimeEntry) {
        timeEntry.workday = workday
        workday.entries.add(timeEntry)

        timeEntryRepository.save(timeEntry)
        dayRepository.save(workday)
        logger.info("Booking was added to day, day={} start={} end={}", workday.date, timeEntry.start, timeEntry.end)
    }

    @Transactional
    fun getBalance(): Status {
        val now = LocalDate.now(clock)
        val workday = dayRepository.findByDate(now) ?: Workday(now)
        return calcStatus(workday)
    }

    @Transactional
    fun getEntries(month: LocalDate): List<Workday> {
        val start = month.withDayOfMonth(1)
        val end = month.with(TemporalAdjusters.lastDayOfMonth())

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
        return ArrayList(map.values.filterNotNull())
    }

    @Transactional
    fun getEntries(typeStr: String?): List<TimeEntry> {
        val type = if (typeStr == "correction") {
            EntryType.CORRECTION
        } else {
            EntryType.STANDARD
        }

        return timeEntryRepository.findAllByType(type)
    }

    @Transactional
    fun getAllEntries(): List<Workday> {
        calcBalance()

        val days = dayRepository.findAllByOrderByDate()
        if (days.isEmpty()) {
            return emptyList()
        }

        val start = days.first().date
        val end = LocalDate.now(clock)
        val map: MutableMap<String, Workday?> = TreeMap()
        val dist = ChronoUnit.DAYS.between(start, end)
        var s = start
        (0..dist).forEach { _ ->
            val date = s
            val key = date.format(DateTimeFormatter.ofPattern(DATE_FORMAT))
            map[key] = null
            s = s.plusDays(1)
        }

        days.forEach { i: Workday ->
            i.isWorkday = isWorkday(i)
            val key = i.date.format(DateTimeFormatter.ofPattern(DATE_FORMAT))
            map[key] = i
        }
        setEmptyDays(map)

        return map.values.toMutableList().filterNotNull()
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

    /**
     * This method retrieves the holidays for a given date.
     *
     * @param date The date for which the holidays are to be retrieved.
     * @return The first holiday that matches the given date, or null if no match is found.
     *
     * The method performs the following steps:
     * 1. Calls the holidayService to get the holidays for the given date.
     * 2. Returns the first holiday that matches the given date.
     * 3. If an exception occurs during the process, it logs the error and returns null.
     */
    fun findHolidaysByDate(date: LocalDate): Holiday? {
        return try {
            holidayService.getHolidays(date).firstOrNull { it.date == date }
        } catch (e: Exception) {
            logger.error("findHolidaysByDate", e)
            null
        }
    }

    @Transactional
    fun delete(id: Long) {
        val entry = timeEntryRepository.findById(id).orElseThrow()
        timeEntryRepository.delete(entry)

        snapshotService.delete()
    }

    @Transactional
    fun deleteAll() {
        timeEntryRepository.deleteAll()
        dayRepository.deleteAll()
        snapshotService.delete()
        logger.info("All bookings and days deleted")
    }


    /** Calculates the current check-in status and totals for a workday. */
    @Transactional
    fun calcStatus(workday: Workday): Status {
        if (workday.entries.isEmpty()) {
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
        )
    }

    /** Loads the worklog, applies deterministic calculations, and persists the resulting totals. */
    @Transactional
    fun calcBalance(): List<Workday> {
        val settings = settingService.get()
        val now = LocalDateTime.now(clock)

        val snapshot = snapshotService.get()

        logger.debug("found snapshot {}", snapshot)

        val snapshotWorkday = snapshot.workday
        val workdays: List<Workday> = if (snapshotWorkday == null) {
            dayRepository.findAllByOrderByDate()
        } else {
            dayRepository.findAllByDateGreaterThanEqualOrderByDate(snapshotWorkday.date)
        }

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
        }
        dayRepository.saveAll(workdays)

        return ArrayList(localDateDayMap.values.filterNotNull())
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
            return false
        }
        val holiday = findHolidaysByDate(workday.date)
        if (holiday != null) {
            return false
        }
        val event = eventService.findByDate(workday.date)
        return event.isEmpty()
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
