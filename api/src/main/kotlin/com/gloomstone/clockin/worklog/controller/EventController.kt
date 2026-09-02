package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.shared.UserPrincipal
import com.gloomstone.clockin.worklog.dto.EventDto
import com.gloomstone.clockin.worklog.mapper.EventMapper
import com.gloomstone.clockin.worklog.service.EventService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
class EventController(
    private val eventService: EventService,
    private val mapper: EventMapper
) {

    @GetMapping("/events")
    operator fun get(@AuthenticationPrincipal principal: UserPrincipal): List<EventDto> {
        val username = principal.username
        val result = eventService.findAllByUsername(username)
        return result.map { event -> this.mapper.toDto(event) }
    }

    @PostMapping("/events")
    fun post(
        @RequestBody request: EventDto,
        @AuthenticationPrincipal principal: UserPrincipal
    ): EventDto {
        val username = principal.username

        val result = eventService.create(request, username)
        return mapper.toDto(result)
    }

    @PutMapping("/events/{id}")
    fun put(
        @RequestBody request: EventDto,
        @PathVariable id: Long,
        @AuthenticationPrincipal principal: UserPrincipal
    ): EventDto {
        val username = principal.username
        if (request.id == null || request.id != id) {
            request.id = id
        }
        return mapper.toDto(eventService.update(request, username))
    }

    @DeleteMapping("/events/{id}")
    fun delete(@PathVariable id: Long?, @AuthenticationPrincipal principal: UserPrincipal) {
        val username = principal.username
        id?.let { eventService.delete(it, username) }
    }
}