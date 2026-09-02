package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.shared.exception.NotFoundException
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
import java.util.Objects.isNull
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
    private val holidayService: HolidayService,
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(WorklogService::class.java)
    private var clock = Clock.system(ZoneId.of("Europe/Berlin"))

    /**
     * Books a day for a user.
     * @param username The user for whom the booking is to be made.
     * @return The booking status after the operation.
     */
    @Transactional
    fun recordEntry(username: String): Status {
        val user = userService.findByIdentity(username) ?: throw BadRequestException("User not found")
        val now = LocalDateTime.now(clock)
        logger.info("user booked, user=\"{}\"", user.username)

        val day = dayRepository.findByDateAndUser(now.toLocalDate(), user) ?: createDay(now.toLocalDate(), user)

        if (day.entries.isEmpty() || day.entries.last().end != null) {
            val timeEntry = TimeEntry(now, day, user)
            addEntry(day, timeEntry)
        } else {
            val last = day.entries.last()
            last.end = now
            logger.info("write booking, booking={}", last.id)
            timeEntryRepository.save(last)
        }
        logger.info("user booked, user=\"{}\"", user.username)
        return calcStatus(day)
    }

    @Transactional
    fun update(request: UpdateWorkdayRequest, username: String): Workday {
        val date = request.date
        val user = this.userService.findByIdentity(username) ?: throw BadRequestException("User not found")
        val day = dayRepository.findByDateAndUser(date, user) ?: createDay(date, user)

        timeEntryRepository.deleteAll(day.entries)

        val newLogEntries = mutableListOf<TimeEntry>()

        for (entry in request.entries) {
            if (entry.type == EntryType.STANDARD.value) {
                if (entry.end != null) {
                    newLogEntries.add(TimeEntry(entry.start!!, entry.end!!, day, user))
                } else {
                    newLogEntries.add(TimeEntry(entry.start!!, day, user))
                }
            } else if (entry.type == EntryType.CORRECTION.value) {
                val duration = entry.duration!!
                newLogEntries.add(TimeEntry(duration, EntryType.CORRECTION, day, user))
            }
        }
        val sortedEntries = getSortedList(newLogEntries)
        timeEntryRepository.saveAll(sortedEntries)

        day.entries = newLogEntries
        day.gross = calcGross(day)


        snapshotService.createSnapshot(date, user)
        calcBalance(user)


        return dayRepository.save(day).also {
            logger.info("day updated, day={} user=\"{}\"", day.date, user.username)
        }
    }

    private fun getSortedList(newLogEntries: List<TimeEntry>): List<TimeEntry> {
        return newLogEntries
            .sortedWith { o1, o2 ->
                if (o1.start != null && o2.start != null) {
                    o1.start!!.compareTo(o2.start)
                } else {
                    1
                }
            }
            .toList()
    }

    @Transactional
    fun createDay(date: LocalDate, user: User): Workday {
        val workday = Workday(date, user)
        return dayRepository.save(workday).also {
            logger.info("day created, date={} user=\"{}\"", date, user.username)
        }
    }

    @Transactional
    fun addEntry(workday: Workday, timeEntry: TimeEntry) {
        timeEntry.workday = workday
        workday.entries.add(timeEntry)

        timeEntryRepository.save(timeEntry)
        dayRepository.save(workday)
        logger.info(
            "Booking was added to day, day={} start={} end={} user=\"{}\"",
            workday.date, timeEntry.start, timeEntry.end, timeEntry.user?.username
        )
    }

    @Transactional
    fun getBalance(username: String): Status {
        val user = this.userService.findByIdentity(username) ?: throw BadRequestException("User not found")
        val now = LocalDate.now(clock)
        val workday = dayRepository.findByDateAndUser(now, user) ?: Workday(now, user)
        return calcStatus(workday)
    }

    @Transactional
    fun getEntries(month: LocalDate, username: String): List<Workday> {
        val user = this.userService.findByIdentity(username) ?: throw BadRequestException("User not found")
        val start = month.withDayOfMonth(1)
        val end = month.with(TemporalAdjusters.lastDayOfMonth())

        calcBalance(user)

        val days = dayRepository.findAllByDateGreaterThanEqualAndDateLessThanEqualAndUserOrderByDate(start, end, user)

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
        setEmptyDays(map, user)
        return ArrayList(map.values.filterNotNull())
    }

    @Transactional
    fun getEntries(typeStr: String?, username: String): List<TimeEntry> {
        val user = this.userService.findByIdentity(username) ?: throw BadRequestException("User not found")
        val type = if (typeStr == "correction") {
            EntryType.CORRECTION
        } else {
            EntryType.STANDARD
        }

        return timeEntryRepository.findAllByTypeAndUser(type, user)
    }

    @Transactional
    fun getAllEntries(username: String): List<Workday> {
        val user = this.userService.findByIdentity(username) ?: throw BadRequestException("User not found")
        calcBalance(user)

        val days = dayRepository.findAllByUserOrderByDate(user)
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
        setEmptyDays(map, user)

        return map.values.toMutableList().filterNotNull()
    }

    private fun setEmptyDays(
        map: MutableMap<String, Workday?>,
        user: User
    ) {
        map.forEach { (key: String, value: Workday?) ->
            if (value == null) {
                val date = LocalDate.parse(key, DateTimeFormatter.ofPattern(DATE_FORMAT))
                val d = Workday(date, user)
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
    fun delete(id: Long, username: String) {
        val user = userService.findByIdentity(username) ?: throw BadRequestException("User not found")
        val entry = timeEntryRepository.findById(id).orElseThrow()
        if (entry.user?.id != user.id) {
            throw NotFoundException("Time entry not found")
        }
        timeEntryRepository.delete(entry)

        val snapshot = snapshotService.findOrCreate(user)
        snapshotService.delete(snapshot)
    }

    @Transactional
    fun deleteAll(username: String) {
        val user = this.userService.findByIdentity(username) ?: throw BadRequestException("User not found")
        timeEntryRepository.deleteAllByUser(user)
        dayRepository.deleteAllByUser(user)
        snapshotService.delete(user)
        logger.info("All bookings and days deleted, user=\"{}\"", user.username)
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
     * 4. If there are bookings, it checks if the user is checked in.
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
        val foundDay = (calcBalance(workday.user).find { it.date == workday.date } ?: workday)
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
     * 1. Retrieves the user and break time from the settings.
     * 2. Retrieves the bookings for the day.
     * 3. If there are no bookings, it returns a duration of zero.
     * 4. If there are bookings, it calculates the duration for each booking and adds them to a list.
     * 5. The durations are then reduced to a single value.
     * 6. If there is more than one booking and the first booking is longer than six hours, the break time is subtracted from the total duration.
     * 7. Returns the calculated gross duration.
     */
    private fun calcGross(workday: Workday): Duration {
        val user = workday.user
        val breakTime = settingService.findByUser(user).breakTime
        val entries = workday.entries
        val now = LocalDateTime.now(clock)
        val startOfDay = now.with(LocalTime.MIDNIGHT)
        val durations: MutableList<Duration> = ArrayList()
        if (isNull(entries) || entries.isEmpty()) {
            return Duration.ZERO
        }

        entries.filter { it.type == EntryType.STANDARD }.forEach { timeEntry: TimeEntry ->
            if (timeEntry.start != null && timeEntry.end != null) {
                durations.add(calcGross(timeEntry))
            } else if (timeEntry.start == null && timeEntry.end == null) {
                durations.add(Duration.ZERO)
            } else {
                if (timeEntry.start!!.isAfter(startOfDay)) {
                    durations.add(Duration.between(timeEntry.start, now))
                }
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

    private fun calcGross(timeEntry: TimeEntry): Duration {
        return Duration.between(timeEntry.start, timeEntry.end)
    }

    /**
     * This method calculates the balance for a given user.
     *
     * @param user The user for whom the balance is to be calculated.
     * @return The list of days with their calculated balances.
     *
     * The method performs the following steps:
     * 1. Retrieves the user's settings and working hours.
     * 2. Retrieves the snapshot for the user.
     * 3. Retrieves the days for the user based on the snapshot.
     * 4. Calculates the days and stores them in a map.
     * 5. Iterates over the days and updates the map with the day's date as the key and the day as the value.
     * 6. Iterates over the entries in the map. For each entry:
     *    - If the value is null, a new day is created and added to the map.
     *    - The gross duration for the day is calculated.
     *    - Checks if the day is a workday and if there are bookings for the day.
     *    - If there are bookings and the user is not checked in, the balance for the day is calculated.
     *    - If there are no bookings or the user is checked in, the balance is set based on the working hours and the previous day's balance.
     *    - If there is a correction, the balance is updated with the correction duration.
     *    - If the day is the same as the snapshot day, the balance is set to the snapshot duration.
     * 7. Saves all the days in the repository.
     * 8. Returns a list of all the days with their calculated balances.
     */
    @Transactional
    fun calcBalance(user: User): List<Workday> {
        val settings = settingService.findByUser(user)

        val workingHours = settings.workingHours
        val now = LocalDate.now(clock)

        val snapshot = snapshotService.findOrCreate(user)

        logger.debug("found snapshot {}", snapshot)

        val workdays: List<Workday> = if (snapshot.workday == null) {
            dayRepository.findAllByUserOrderByDate(user)
        } else {
            snapshot.workday?.date.let {
                dayRepository.findAllByDateGreaterThanEqualAndUserOrderByDate(it, user)
            }
        }

        val localDateDayMap = calcDays(workdays)

        for (d in workdays) {
            val key = d.date
            localDateDayMap[key] = d
        }

        var prev: Workday? = null

        val snapshotDuration = snapshot.workday?.balance ?: Duration.ZERO

        for (entry in localDateDayMap.entries) {
            var value = entry.value
            if (value == null) {
                value = createDay(entry.key, user)

                localDateDayMap[entry.key] = value
            }
            val correction = entry.value?.entries?.find { i -> i.type == EntryType.CORRECTION }

            value.gross = calcGross(value)

            value.isWorkday = isWorkday(value)
            if (hasEntries(value) && !isCheckedIn(value)) {
                value.balance = getBalanceWithoutBeingCheckedIn(prev, value, workingHours)
            } else {
                value.balance = getBalanceWithBeingCheckedIn(value, now, prev, workingHours)
            }
            if (correction != null) {
                value.balance = value.balance!!.plus(correction.duration!!.toDuration())
            }
            if (snapshot.workday != null && value.date.isEqual(snapshot.workday!!.date)) {
                value.balance = snapshotDuration
            }

            prev = value
        }
        dayRepository.saveAll(workdays)

        return ArrayList(localDateDayMap.values.filterNotNull())
    }

    private fun getBalanceWithBeingCheckedIn(
        value: Workday,
        now: LocalDate,
        prev: Workday?,
        workingHours: Duration
    ) = if (value.isWorkday && value.date != now) {
        if (prev == null) {
            workingHours.negated()
        } else {
            prev.balance!!.minus(workingHours)
        }
    } else {
        if (prev == null) {
            value.balance ?: Duration.ZERO
        } else {
            prev.balance
        }
    }

    private fun getBalanceWithoutBeingCheckedIn(
        prev: Workday?,
        value: Workday,
        workingHours: Duration
    ): Duration? {
        return if (prev == null) {
            if (value.isWorkday) {
                value.gross!!.minus(workingHours)
            } else {
                value.gross
            }
        } else {
            if (value.isWorkday) {
                if (value.gross!!.isNegative) {
                    prev.balance!!.plus(value.gross)
                } else {
                    value.gross!!.minus(workingHours).plus(prev.balance)
                }
            } else {
                value.gross!!.plus(prev.balance)
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
        val settings = settingService.findByUser(workday.user)
        val workDay =
            settings.workingDays and 2.0.pow((workday.date.dayOfWeek.value - 1).toDouble()).toInt().toLong() != 0L
        if (!workDay) {
            return false
        }
        val holiday = findHolidaysByDate(workday.date)
        if (holiday != null) {
            return false
        }
        val event = eventService.findByDateAndUsername(
            workday.date, workday.user.username
        )
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
