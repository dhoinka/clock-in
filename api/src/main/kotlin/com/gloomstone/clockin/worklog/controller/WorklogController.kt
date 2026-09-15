package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.worklog.dto.StatusResponse
import com.gloomstone.clockin.worklog.dto.TimeEntryResponse
import com.gloomstone.clockin.worklog.dto.UpdateWorkdayRequest
import com.gloomstone.clockin.worklog.dto.WorkdayResponse
import com.gloomstone.clockin.worklog.mapper.WorklogMapper
import com.gloomstone.clockin.worklog.service.WorklogService
import org.springframework.web.bind.annotation.*
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
class WorklogController(
    private val service: WorklogService,
    private val mapper: WorklogMapper,
    private val clock: Clock,
) {
    @PostMapping("/status")
    fun post(): StatusResponse {
        return mapper.toDto(service.recordEntry())
    }

    @GetMapping("/status")
    fun get(): StatusResponse {
        return mapper.toDto(service.getBalance())
    }

    @PutMapping("/entries")
    fun put(@RequestBody body: UpdateWorkdayRequest): WorkdayResponse {
        return mapper.toDto(service.update(body))
    }


    @GetMapping("/workdays/{month}", "/workdays")
    fun getAll(@PathVariable(required = false) month: String?): List<WorkdayResponse> {
        return service.getEntries(parseMonth(month))
            .map(mapper::toDto)
    }

    @GetMapping("/entries")
    fun entries(@RequestParam(required = false) type: String?): List<TimeEntryResponse> {
        return service.getEntries(type)
            .map(mapper::toDto)
    }

    @DeleteMapping("/entries/{id}")
    fun delete(@PathVariable id: Long) {
        return service.delete(id)
    }

    @DeleteMapping("/workdays")
    fun deleteAll() {
        return service.deleteAll()
    }

    private fun parseMonth(month: String?): LocalDate {
        return month?.let { LocalDate.parse("$it-01", DateTimeFormatter.ISO_DATE) } ?: LocalDate.now(clock)
    }

}
