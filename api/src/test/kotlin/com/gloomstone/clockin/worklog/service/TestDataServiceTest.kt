package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.domain.TimeEntry
import com.gloomstone.clockin.worklog.domain.Workday
import com.gloomstone.clockin.worklog.repository.DayRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestDataServiceTest {
    private val entries: TimeEntryRepository = mock()
    private val days: DayRepository = mock()
    private val service = TestDataService(entries, days)

    @Test
    fun `creates thirty global workdays when the worklog is empty`() {
        whenever(days.count()).thenReturn(0)
        whenever(days.save(any<Workday>())).thenAnswer { it.arguments[0] }

        service.createTestData()

        verify(days, times(30)).save(any<Workday>())
        verify(entries, times(30)).save(any<TimeEntry>())
    }

    @Test
    fun `does not add sample data to a non-empty worklog`() {
        whenever(days.count()).thenReturn(1)

        service.createTestData()

        verify(days, never()).save(any<Workday>())
        verify(entries, never()).save(any<TimeEntry>())
    }
}
