package com.gloomstone.clockin.worklog.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class HolidaySyncJobTest {
    private val holidayService = mock<HolidayService>()
    private val clock = Clock.fixed(Instant.parse("2026-09-17T08:00:00Z"), ZoneOffset.UTC)
    private val job = HolidaySyncJob(holidayService, clock, true)

    @Test
    fun `startup synchronizes the current and next year`() {
        job.run(mock())

        inOrder(holidayService) {
            verify(holidayService).ensureYearLoaded(2026)
            verify(holidayService).ensureYearLoaded(2027)
        }
    }
}
