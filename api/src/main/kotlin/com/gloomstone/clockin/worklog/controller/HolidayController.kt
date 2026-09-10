package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.worklog.domain.Holiday
import com.gloomstone.clockin.worklog.service.HolidayService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.LocalDate

@RestController
class HolidayController(
    private val holidayService: HolidayService,
    private val clock: Clock,
) {
    @GetMapping("/holidays/{year}", "/holidays")
    fun get(@PathVariable(required = false) year: String?): List<Holiday> {
        val now = if (year == null) LocalDate.now(clock) else LocalDate.of(year.toInt(), 1, 1)
        return holidayService.getHolidays(now)
    }
}
