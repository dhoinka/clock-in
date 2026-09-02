package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.shared.UserPrincipal
import com.gloomstone.clockin.worklog.domain.Stat
import com.gloomstone.clockin.worklog.dto.StatusResponse
import com.gloomstone.clockin.worklog.dto.TimeEntryResponse
import com.gloomstone.clockin.worklog.dto.UpdateWorkdayRequest
import com.gloomstone.clockin.worklog.dto.WorkdayResponse
import com.gloomstone.clockin.worklog.mapper.WorklogMapper
import com.gloomstone.clockin.worklog.service.StatService
import com.gloomstone.clockin.worklog.service.WorklogService
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
class WorklogController(
    private val worklogService: WorklogService,
    private val statService: StatService,
    private val mapper: WorklogMapper
) {
    private val logger = LoggerFactory.getLogger(WorklogController::class.java)

    @PostMapping("/status")
    fun post(@AuthenticationPrincipal principal: UserPrincipal): StatusResponse {
        logger.info("time entry request received, user=\"{}\"", principal.username)
        val username = principal.username
        return worklogService.recordEntry(username)
            .let { mapper.toDto(it) }
    }

    @GetMapping("/status")
    operator fun get(@AuthenticationPrincipal principal: UserPrincipal): StatusResponse {
        val username = principal.username
        return worklogService.getBalance(username)
            .let { mapper.toDto(it) }
    }

    @PutMapping("/entries")
    fun put(
        @RequestBody body: UpdateWorkdayRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): WorkdayResponse {
        return principal.username
            .let { worklogService.update(body, it) }
            .let { mapper.toDto(it) }
    }


    @GetMapping(value = ["/workdays/{month}", "/workdays"])
    fun getAll(
        @PathVariable(required = false) month: String?, @AuthenticationPrincipal principal: UserPrincipal
    ): List<WorkdayResponse> {
        return principal.username
            .let { user ->
                val start = LocalDateTime.now()
                val monthDate: LocalDate = if (month == null) {
                    LocalDate.now()
                } else {
                    LocalDate.parse("$month-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }

                val days = worklogService.getEntries(monthDate, user)

                val end = LocalDateTime.now()
                logger.debug("getAll: {}ms", Duration.between(start, end).toMillis())
                days
            }
            .map { mapper.toDto(it) }
    }

    @GetMapping("/workdays/export")
    fun export(@AuthenticationPrincipal principal: UserPrincipal): List<WorkdayResponse> {
        return principal.username
            .let { worklogService.getAllEntries(it) }
            .map { mapper.toDto(it) }
    }

    @GetMapping("/entries")
    fun getEntries(
        @RequestParam(required = false) type: String?,
        @AuthenticationPrincipal principal: UserPrincipal
    ): List<TimeEntryResponse> {
        return principal.username
            .let { worklogService.getEntries(type, it) }
            .map { mapper.toDto(it) }
    }

    @GetMapping("/worklog/stats")
    fun getStats(@AuthenticationPrincipal principal: UserPrincipal): Stat {
        return principal.username
            .let { statService.getStats(it) }
    }

    @DeleteMapping("/entries/{id}")
    fun delete(
        @PathVariable id: String?,
        @AuthenticationPrincipal principal: UserPrincipal
    ) {
        if (!id.isNullOrBlank()) {
            val anId = id.toLong()
            val username = principal.username
            this.worklogService.delete(anId, username)
        }
    }

    @DeleteMapping("/workdays")
    fun deleteAll(@AuthenticationPrincipal principal: UserPrincipal) {
        worklogService.deleteAll(principal.username)
    }


}
