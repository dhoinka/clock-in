package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.worklog.domain.*
import com.gloomstone.clockin.worklog.dto.TimeEntryResponse
import com.gloomstone.clockin.worklog.dto.UpdateWorkdayRequest
import com.gloomstone.clockin.worklog.repository.DayRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.*

class WorklogServiceTest {
    private val timeEntryRepository = mock<TimeEntryRepository>()
    private val dayRepository = mock<DayRepository>()
    private val snapshotService = mock<SnapshotService>()
    private val settingService = mock<SettingService>()
    private val eventService = mock<EventService>()
    private val holidayService = mock<HolidayService>()
    private val userService = mock<UserService>()
    private val service = WorklogService(
        timeEntryRepository,
        dayRepository,
        snapshotService,
        settingService,
        eventService,
        holidayService,
        userService,
    )
    private val user = User("alice@example.org", "alice", "Alice", id = "alice-id")
    private val day = LocalDate.of(2026, 9, 3)

    @BeforeEach
    fun setUp() {
        service.setClock(
            Clock.fixed(
                day.atTime(8, 0).atZone(ZoneId.of("Europe/Berlin")).toInstant(),
                ZoneId.of("Europe/Berlin")
            )
        )
        whenever(userService.findByIdentity(user.username)).thenReturn(user)
        whenever(settingService.findByUser(user)).thenReturn(
            Setting(Duration.ofHours(8), Duration.ofMinutes(30), 31, user)
        )
        whenever(snapshotService.findOrCreate(user)).thenReturn(Snapshot(user = user))
        whenever(holidayService.getHolidays(any())).thenReturn(emptyList())
        whenever(eventService.findByDateAndUsername(any(), any())).thenReturn(emptyList())
        whenever(dayRepository.save<Workday>(any())).thenAnswer { it.getArgument<Workday>(0) }
        whenever(dayRepository.saveAll<Workday>(any<List<Workday>>())).thenAnswer { it.getArgument<List<Workday>>(0) }
        whenever(timeEntryRepository.save<TimeEntry>(any())).thenAnswer { it.getArgument<TimeEntry>(0) }
        whenever(timeEntryRepository.saveAll<TimeEntry>(any<List<TimeEntry>>())).thenAnswer {
            it.getArgument<List<TimeEntry>>(
                0
            )
        }
    }

    @Test
    fun `recordEntry creates a workday and an open standard entry`() {
        var savedDay: Workday? = null
        whenever(dayRepository.findByDateAndUser(day, user)).thenReturn(null)
        whenever(dayRepository.save<Workday>(any())).thenAnswer {
            it.getArgument<Workday>(0).also { persisted -> savedDay = persisted }
        }
        whenever(dayRepository.findAllByUserOrderByDate(user)).thenAnswer { listOfNotNull(savedDay) }

        val status = service.recordEntry(user.username)

        val persisted = requireNotNull(savedDay)
        assertThat(status.isCheckedIn).isTrue()
        assertThat(status.gross).isZero()
        val entry = persisted.entries.single()
        assertThat(entry.type).isEqualTo(EntryType.STANDARD)
        assertThat(entry.start).isEqualTo(LocalDateTime.of(day, LocalTime.of(8, 0)))
        assertThat(entry.end).isNull()
        assertThat(entry.user).isSameAs(user)
        verify(timeEntryRepository).save(entry)
    }

    @Test
    fun `recordEntry closes the last open entry and reports its gross duration`() {
        val openEntry = TimeEntry(LocalDateTime.of(day, LocalTime.of(8, 0)), Workday(day, user), user)
        val workday = openEntry.workday!!.apply { entries.add(openEntry) }
        whenever(dayRepository.findByDateAndUser(day, user)).thenReturn(workday)
        whenever(dayRepository.findAllByUserOrderByDate(user)).thenReturn(listOf(workday))
        service.setClock(
            Clock.fixed(
                day.atTime(10, 0).atZone(ZoneId.of("Europe/Berlin")).toInstant(),
                ZoneId.of("Europe/Berlin")
            )
        )

        val status = service.recordEntry(user.username)

        assertThat(openEntry.end).isEqualTo(LocalDateTime.of(day, LocalTime.of(10, 0)))
        assertThat(status.isCheckedIn).isFalse()
        assertThat(status.gross).isEqualTo(Duration.ofHours(2))
        verify(timeEntryRepository).save(openEntry)
    }

    @Test
    fun `update replaces entries and persists them in start-time order`() {
        val existing = Workday(day, user)
        val oldEntry = TimeEntry(LocalDateTime.of(day, LocalTime.of(8, 0)), existing, user)
        existing.entries.add(oldEntry)
        whenever(dayRepository.findByDateAndUser(day, user)).thenReturn(existing)
        whenever(dayRepository.findAllByUserOrderByDate(user)).thenReturn(listOf(existing))
        val request = UpdateWorkdayRequest(
            day,
            listOf(
                TimeEntryResponse(
                    "standard",
                    LocalDateTime.of(day, LocalTime.of(13, 0)),
                    LocalDateTime.of(day, LocalTime.of(17, 0))
                ),
                TimeEntryResponse("correction", duration = "1h"),
                TimeEntryResponse(
                    "standard",
                    LocalDateTime.of(day, LocalTime.of(8, 0)),
                    LocalDateTime.of(day, LocalTime.of(12, 0))
                ),
            ),
        )
        val entriesCaptor = argumentCaptor<List<TimeEntry>>()

        val updated = service.update(request, user.username)

        verify(timeEntryRepository).deleteAll(listOf(oldEntry))
        verify(timeEntryRepository).saveAll(entriesCaptor.capture())
        assertThat(entriesCaptor.firstValue.filter { it.start != null }.map { it.start })
            .containsExactly(LocalDateTime.of(day, LocalTime.of(8, 0)), LocalDateTime.of(day, LocalTime.of(13, 0)))
        assertThat(entriesCaptor.firstValue.single { it.type == EntryType.CORRECTION }.duration).isEqualTo("1h")
        assertThat(updated.entries).hasSize(3)
        assertThat(updated.gross).isEqualTo(Duration.ofHours(8))
        verify(snapshotService).createSnapshot(day, user)
    }

    @Test
    fun `recordEntry rejects an unknown user before writing a day`() {
        whenever(userService.findByIdentity("missing")).thenReturn(null)

        assertThatThrownBy { service.recordEntry("missing") }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessage("User not found")
        verify(dayRepository, never()).save(any())
        verify(timeEntryRepository, never()).save(any())
    }
}
