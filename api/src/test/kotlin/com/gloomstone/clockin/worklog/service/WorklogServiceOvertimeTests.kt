package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.worklog.domain.*
import com.gloomstone.clockin.worklog.repository.DayRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.*

class WorklogServiceOvertimeTests {
    private val user = User("alice@example.org", "alice", "Alice", id = "alice-id")

    @Test
    fun `calcBalance carries daily overtime across consecutive workdays`() {
        val start = LocalDate.of(2021, 12, 1)
        val days = (0..2).map { normalDay(start.plusDays(it.toLong())) }
        val fixture = fixture(days, start.plusDays(2))

        val result = fixture.service.calcBalance(user)

        assertThat(result.map { it.balance })
            .containsExactly(Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(1).plusMinutes(30))
    }

    @Test
    fun `calcBalance applies corrections after daily work time`() {
        val start = LocalDate.of(2021, 12, 1)
        val firstDay = normalDay(start).also { workday ->
            workday.entries.add(TimeEntry("5h", EntryType.CORRECTION, workday, user))
        }
        val fixture =
            fixture(listOf(firstDay, normalDay(start.plusDays(1)), normalDay(start.plusDays(2))), start.plusDays(2))

        val result = fixture.service.calcBalance(user)

        assertThat(result.map { it.gross })
            .containsExactly(
                Duration.ofHours(8).plusMinutes(30),
                Duration.ofHours(8).plusMinutes(30),
                Duration.ofHours(8).plusMinutes(30)
            )
        assertThat(result.map { it.balance })
            .containsExactly(
                Duration.ofHours(5).plusMinutes(30),
                Duration.ofHours(6),
                Duration.ofHours(6).plusMinutes(30)
            )
    }

    @Test
    fun `calcBalance does not subtract target hours on a holiday`() {
        val start = LocalDate.of(2026, 9, 7)
        val fixture = fixture(listOf(normalDay(start), normalDay(start.plusDays(1))), start.plusDays(1))
        whenever(fixture.holidayService.getHolidays(any())).thenReturn(listOf(Holiday(start, "Holiday")))

        val result = fixture.service.calcBalance(user)

        assertThat(result.map { it.isWorkday }).containsExactly(false, true)
        assertThat(result.map { it.balance }).containsExactly(Duration.ofHours(8).plusMinutes(30), Duration.ofHours(9))
    }

    private fun normalDay(date: LocalDate): Workday {
        val workday = Workday(date, user)
        workday.entries.add(
            TimeEntry(
                LocalDateTime.of(date, LocalTime.of(8, 0)),
                LocalDateTime.of(date, LocalTime.of(17, 0)),
                workday,
                user,
            )
        )
        return workday
    }

    private fun fixture(days: List<Workday>, today: LocalDate): Fixture {
        val dayRepository = mock<DayRepository>()
        val timeEntryRepository = mock<TimeEntryRepository>()
        val snapshotService = mock<SnapshotService>()
        val settingService = mock<SettingService>()
        val eventService = mock<EventService>()
        val holidayService = mock<HolidayService>()
        val userService = mock<com.gloomstone.clockin.iam.service.UserService>()
        whenever(settingService.findByUser(user)).thenReturn(
            Setting(
                Duration.ofHours(8),
                Duration.ofMinutes(30),
                31,
                user
            )
        )
        whenever(snapshotService.findOrCreate(user)).thenReturn(Snapshot(user = user))
        whenever(dayRepository.findAllByUserOrderByDate(user)).thenReturn(days)
        whenever(dayRepository.saveAll<Workday>(any<List<Workday>>())).thenAnswer { it.getArgument<List<Workday>>(0) }
        whenever(holidayService.getHolidays(any())).thenReturn(emptyList())
        whenever(eventService.findByDateAndUsername(any(), any())).thenReturn(emptyList())

        val service = WorklogService(
            timeEntryRepository,
            dayRepository,
            snapshotService,
            settingService,
            eventService,
            holidayService,
            userService,
        )
        service.setClock(
            Clock.fixed(today.atTime(12, 0).atZone(ZoneId.of("Europe/Berlin")).toInstant(), ZoneId.of("Europe/Berlin"))
        )
        return Fixture(service, holidayService)
    }

    private data class Fixture(val service: WorklogService, val holidayService: HolidayService)
}
