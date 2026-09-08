package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.domain.EntryType
import com.gloomstone.clockin.worklog.domain.Setting
import com.gloomstone.clockin.worklog.domain.Snapshot
import com.gloomstone.clockin.worklog.domain.TimeEntry
import com.gloomstone.clockin.worklog.domain.Workday
import com.gloomstone.clockin.worklog.dto.TimeEntryResponse
import com.gloomstone.clockin.worklog.dto.UpdateWorkdayRequest
import com.gloomstone.clockin.worklog.repository.DayRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

class WorklogServiceRegressionTest {
    private val entries: TimeEntryRepository = mock()
    private val days: DayRepository = mock()
    private val snapshots: SnapshotService = mock()
    private val settings: SettingService = mock()
    private val events: EventService = mock()
    private val holidays: HolidayService = mock()
    private lateinit var service: WorklogService
    private val date = LocalDate.of(2026, 9, 8)

    @BeforeEach
    fun setUp() {
        service = WorklogService(entries, days, snapshots, settings, events, holidays)
        service.setClock(Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"), ZoneId.of("Europe/Berlin")))
        whenever(settings.get()).thenReturn(Setting(Duration.ofHours(8), Duration.ofMinutes(30), 31))
        whenever(snapshots.get()).thenReturn(Snapshot())
        whenever(events.findByDate(any())).thenReturn(emptyList())
        whenever(holidays.getHolidays(any())).thenReturn(emptyList())
        whenever(days.save(any<Workday>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `global update deducts configured break from a standard entry over six hours`() {
        val day = Workday(date)
        whenever(days.findByDate(date)).thenReturn(day)
        whenever(days.findAllByOrderByDate()).thenReturn(listOf(day))
        val request = UpdateWorkdayRequest(date, listOf(
            TimeEntryResponse(EntryType.STANDARD.value, LocalDateTime.of(2026, 9, 8, 8, 0), LocalDateTime.of(2026, 9, 8, 15, 0))
        ))

        val updated = service.update(request)

        assertThat(updated.gross).isEqualTo(Duration.ofHours(6).plusMinutes(30))
        assertThat(updated.balance).isEqualTo(Duration.ofHours(-1).plusMinutes(-30))
        verify(snapshots).createSnapshot(date)
    }

    @Test
    fun `global update applies correction entries to the running balance`() {
        val day = Workday(date)
        whenever(days.findByDate(date)).thenReturn(day)
        whenever(days.findAllByOrderByDate()).thenReturn(listOf(day))
        val request = UpdateWorkdayRequest(date, listOf(
            TimeEntryResponse(EntryType.STANDARD.value, LocalDateTime.of(2026, 9, 8, 8, 0), LocalDateTime.of(2026, 9, 8, 12, 0)),
            TimeEntryResponse(EntryType.STANDARD.value, LocalDateTime.of(2026, 9, 8, 12, 0), LocalDateTime.of(2026, 9, 8, 16, 0)),
            TimeEntryResponse(EntryType.CORRECTION.value, duration = "-1h")
        ))

        val updated = service.update(request)

        assertThat(updated.balance).isEqualTo(Duration.ofHours(-1))
    }

    @Test
    fun `delete operations are global and do not require ownership`() {
        val entry = TimeEntry(LocalDateTime.of(2026, 9, 8, 8, 0), Workday(date))
        whenever(entries.findById(42)).thenReturn(Optional.of(entry))

        service.delete(42)
        service.deleteAll()

        verify(entries).delete(entry)
        verify(entries).deleteAll()
        verify(days).deleteAll()
        verify(snapshots, times(2)).delete()
    }
}
