package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.worklog.dto.UpdateWorkdayRequest
import com.gloomstone.clockin.worklog.mapper.WorklogMapper
import com.gloomstone.clockin.worklog.service.StatService
import com.gloomstone.clockin.worklog.service.WorklogService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
class WorklogController(
    private val service: WorklogService,
    private val mapper: WorklogMapper,
    private val statService: StatService,
    private val clock: Clock,
) {
    @PostMapping("/status")
    fun post() = mapper.toDto(service.recordEntry())

    @GetMapping("/status")
    fun get() = mapper.toDto(service.getBalance())

    @PutMapping("/entries")
    fun put(@RequestBody body: UpdateWorkdayRequest) = mapper.toDto(service.update(body))

    @GetMapping("/workdays/{month}", "/workdays")
    fun getAll(@PathVariable(required = false) month: String?) =
        service.getEntries(parseMonth(month)).map(mapper::toDto)

    @GetMapping("/workdays/export")
    fun export() = service.getAllEntries().map(mapper::toDto)

    @GetMapping("/entries")
    fun entries(@RequestParam(required = false) type: String?) =
        service.getEntries(type).map(mapper::toDto)

    @GetMapping("/worklog/stats")
    fun stats() = statService.getStats()

    @DeleteMapping("/entries/{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

    @DeleteMapping("/workdays")
    fun deleteAll() = service.deleteAll()

    private fun parseMonth(month: String?): LocalDate =
        month?.let { LocalDate.parse("$it-01", DateTimeFormatter.ISO_DATE) } ?: LocalDate.now(clock)
}
