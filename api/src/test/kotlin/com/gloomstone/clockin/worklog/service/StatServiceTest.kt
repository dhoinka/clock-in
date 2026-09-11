package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.domain.TimeEntry
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class StatServiceTest {
    private val entries: TimeEntryRepository = mock()
    private val service = StatService(entries)

    @Test
    fun `stats average every completed entry globally`() {
        whenever(entries.findAll()).thenReturn(listOf(
            TimeEntry(LocalDateTime.of(2026, 9, 1, 8, 30), LocalDateTime.of(2026, 9, 1, 17, 0), mock()),
            TimeEntry(LocalDateTime.of(2026, 9, 2, 9, 30), LocalDateTime.of(2026, 9, 2, 18, 0), mock()),
            TimeEntry(LocalDateTime.of(2026, 9, 2, 10, 0), mock()),
        ))

        val stat = service.getStats()

        assertThat(stat.avgStart).isEqualTo(9.0)
        assertThat(stat.avgEnd).isEqualTo(17.5)
    }

    @Test
    fun `stats return empty averages with no completed entries`() {
        whenever(entries.findAll()).thenReturn(emptyList())

        assertThat(service.getStats()).extracting("avgStart", "avgEnd").containsExactly(null, null)
    }
}
