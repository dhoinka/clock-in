package com.gloomstone.clockin.worklog.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate

/** Keeps the current and upcoming year's holidays available without frontend coordination. */
@Component
class HolidaySyncJob(
    private val holidayService: HolidayService,
    private val clock: Clock,
    @param:Value("\${holiday.sync.startup-enabled:true}")
    private val startupEnabled: Boolean,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (startupEnabled) {
            syncCurrentAndNextYear()
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    fun syncCurrentAndNextYear() {
        val currentYear = LocalDate.now(clock).year
        holidayService.ensureYearLoaded(currentYear)
        holidayService.ensureYearLoaded(currentYear + 1)
    }
}
