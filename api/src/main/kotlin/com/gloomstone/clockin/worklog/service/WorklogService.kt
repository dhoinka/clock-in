package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.domain.*
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
import kotlin.math.pow

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
    private val holidayService: HolidayService
) {
    private val logger = LoggerFactory.getLogger(WorklogService::class.java)
    private var clock = Clock.system(ZoneId.of("Europe/Berlin"))

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
        val day = dayRepository.findByDate(date) ?: createDay(date)

        timeEntryRepository.deleteAll(day.entries)

        val newLogEntries = mutableListOf<TimeEntry>()

        for (entry in request.entries) {
            if (entry.type == EntryType.STANDARD.value) {
                val start = requireNotNull(entry.start) { "Standard entries require a start time" }
                val end = entry.end
                if (end != null) {
                    newLogEntries.add(TimeEntry(start, end, day))
                } else {
                    newLogEntries.add(TimeEntry(start, day))
                }
            } else if (entry.type == EntryType.CORRECTION.value) {
                val duration = requireNotNull(entry.duration) { "Correction entries require a duration" }
                newLogEntries.add(TimeEntry(duration, EntryType.CORRECTION, day))
            }
        }
        val sortedEntries = getSortedList(newLogEntries)
        timeEntryRepository.saveAll(sortedEntries)

        day.entries = newLogEntries
        day.gross = calcGross(day)


        snapshotService.createSnapshot(date)
        calcBalance()


        return dayRepository.save(day).also {
            logger.info("day updated, day={}", day.date)
        }
    }

    private fun getSortedList(newLogEntries: List<TimeEntry>): List<TimeEntry> {
        return newLogEntries
            .sortedWith(compareBy<TimeEntry> { it.start == null }.thenBy { it.start })
            .toList()
    }

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
        val end = LocalDate.now()


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


    fun setClock(clock: Clock) {
        this.clock = clock
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


    /**
     * This method calculates the booking status for a given day.
     *
     * @param workday The day for which the booking status is to be calculated.
     * @return The booking status for the given day.
     *
     * The method performs the following steps:
     * 1. Calculates the balance for the given day.
     * 2. Checks if there are any bookings for the day.
     * 3. If there are no bookings, it returns a BookingStatus with false for checkedIn, zero duration for gross and balance.
     * 4. If there are bookings, it checks whether the worklog is checked in.
     * 5. Calculates the gross duration for the day.
     * 6. Returns a BookingStatus with the checkedIn status, gross duration, and the balance for the day.
     */
    @Transactional
    fun calcStatus(workday: Workday): Status {
        if (!hasEntries(workday)) {
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
            foundDay.gross,
            foundDay.balance
        )
    }

    /**
     * This method calculates the gross duration for a given day.
     *
     * @param workday The day for which the gross duration is to be calculated.
     * @return The gross duration for the given day.
     *
     * The method performs the following steps:
     * 1. Retrieves the break time from the settings.
     * 2. Retrieves the bookings for the day.
     * 3. If there are no bookings, it returns a duration of zero.
     * 4. If there are bookings, it calculates the duration for each booking and adds them to a list.
     * 5. The durations are then reduced to a single value.
     * 6. If there is more than one booking and the first booking is longer than six hours, the break time is subtracted from the total duration.
     * 7. Returns the calculated gross duration.
     */
    private fun calcGross(workday: Workday): Duration {
        val breakTime = settingService.get().breakTime
        val entries = workday.entries
        val now = LocalDateTime.now(clock)
        val startOfDay = now.with(LocalTime.MIDNIGHT)
        val durations: MutableList<Duration> = ArrayList()
        if (entries.isEmpty()) {
            return Duration.ZERO
        }

        entries.filter { it.type == EntryType.STANDARD }.forEach { timeEntry: TimeEntry ->
            val start = timeEntry.start
            val end = timeEntry.end

            when {
                start == null -> durations.add(Duration.ZERO)
                end != null -> durations.add(Duration.between(start, end))
                start.isAfter(startOfDay) -> durations.add(Duration.between(start, now))
            }
        }
        // reduce to single value
        var duration = durations.fold(Duration.ZERO, Duration::plus)

        // do I have more than one booking and is larger than six hours? subtract break time
        if (durations.size >= 1 && durations[0] > SIX_HOURS) {
            duration = duration.minus(breakTime)
        }

        return duration
    }

    /**
     * This method calculates the balance for the local worklog.
     * @return The list of days with their calculated balances.
     *
     * The method performs the following steps:
     * 1. Retrieves the settings and working hours.
     * 2. Retrieves the balance snapshot.
     * 3. Retrieves the days based on the snapshot.
     * 4. Calculates the days and stores them in a map.
     * 5. Iterates over the days and updates the map with the day's date as the key and the day as the value.
     * 6. Iterates over the entries in the map. For each entry:
     *    - If the value is null, a new day is created and added to the map.
     *    - The gross duration for the day is calculated.
     *    - Checks if the day is a workday and if there are bookings for the day.
     *    - If there are bookings and the worklog is not checked in, the balance for the day is calculated.
     *    - If there are no bookings or the worklog is checked in, the balance is set based on the working hours and the previous day's balance.
     *    - If there is a correction, the balance is updated with the correction duration.
     *    - If the day is the same as the snapshot day, the balance is set to the snapshot duration.
     * 7. Saves all the days in the repository.
     * 8. Returns a list of all the days with their calculated balances.
     */
    @Transactional
    fun calcBalance(): List<Workday> {
        val settings = settingService.get()

        val workingHours = settings.workingHours
        val now = LocalDate.now(clock)

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
            val correction = entry.value?.entries?.find { i -> i.type == EntryType.CORRECTION }

            val gross = calcGross(value)
            value.gross = gross

            value.isWorkday = isWorkday(value)
            var balance = if (hasEntries(value) && !isCheckedIn(value)) {
                getBalanceWithoutBeingCheckedIn(previousBalance, value, gross, workingHours)
            } else {
                getBalanceWithBeingCheckedIn(value, now, previousBalance, workingHours)
            }

            correction?.let {
                val duration = requireNotNull(it.duration) { "Correction entries require a duration" }
                balance = balance.plus(duration.toDuration())
            }
            if (value.date == snapshotWorkday?.date) {
                balance = snapshotDuration
            }

            value.balance = balance
            previousBalance = balance
        }
        dayRepository.saveAll(workdays)

        return ArrayList(localDateDayMap.values.filterNotNull())
    }

    private fun getBalanceWithBeingCheckedIn(
        value: Workday,
        now: LocalDate,
        previousBalance: Duration?,
        workingHours: Duration
    ) = if (value.isWorkday && value.date != now) {
        previousBalance?.minus(workingHours) ?: workingHours.negated()
    } else {
        previousBalance ?: value.balance ?: Duration.ZERO
    }

    private fun getBalanceWithoutBeingCheckedIn(
        previousBalance: Duration?,
        value: Workday,
        gross: Duration,
        workingHours: Duration
    ): Duration {
        return if (previousBalance == null) {
            if (value.isWorkday) {
                gross.minus(workingHours)
            } else {
                gross
            }
        } else {
            if (value.isWorkday) {
                if (gross.isNegative) {
                    previousBalance.plus(gross)
                } else {
                    gross.minus(workingHours).plus(previousBalance)
                }
            } else {
                gross.plus(previousBalance)
            }
        }
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

    private fun hasEntries(workday: Workday?): Boolean {
        return workday?.entries?.isNotEmpty() ?: false
    }

    private fun isWorkday(workday: Workday): Boolean {
        // Mo Di Mi Do Fr Sa So
        // 1  2  4  8  16 32 64
        val settings = settingService.get()
        val workDay =
            settings.workingDays and 2.0.pow((workday.date.dayOfWeek.value - 1).toDouble()).toInt().toLong() != 0L
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
        if (workday.entries.isEmpty()) {
            return false
        }
        return workday.entries.any { timeEntry: TimeEntry -> timeEntry.start != null && timeEntry.end == null }
    }


    companion object {
        private val SIX_HOURS = Duration.ofHours(6)
        const val DATE_FORMAT = "yyyy-MM-dd"
    }
}
