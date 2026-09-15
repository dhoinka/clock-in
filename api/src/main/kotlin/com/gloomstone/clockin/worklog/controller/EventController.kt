package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.worklog.dto.EventDto
import com.gloomstone.clockin.worklog.mapper.EventMapper
import com.gloomstone.clockin.worklog.service.EventService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
class EventController(
    private val service: EventService,
    private val mapper: EventMapper,
) {
    @GetMapping("/events")
    fun get(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): List<EventDto> {
        val events = when {
            from == null && to == null -> service.findAll()
            from != null && to != null -> service.findAll(from, to)
            else -> throw BadRequestException("Both from and to dates are required when filtering events")
        }
        return events.map(mapper::toDto)
    }

    @PostMapping("/events")
    fun post(@RequestBody request: EventDto): EventDto {
        return mapper.toDto(service.create(request))
    }

    @PutMapping("/events/{id}")
    fun put(@RequestBody request: EventDto, @PathVariable id: Long): EventDto {
        request.id = id
        return mapper.toDto(service.update(request))
    }

    @DeleteMapping("/events/{id}")
    fun delete(@PathVariable id: Long) {
        return service.delete(id)
    }
}
