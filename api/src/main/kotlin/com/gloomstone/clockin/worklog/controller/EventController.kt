package com.gloomstone.clockin.worklog.controller
import com.gloomstone.clockin.worklog.dto.EventDto
import com.gloomstone.clockin.worklog.mapper.EventMapper
import com.gloomstone.clockin.worklog.service.EventService
import org.springframework.web.bind.annotation.*
@RestController class EventController(private val service: EventService, private val mapper: EventMapper) {
 @GetMapping("/events") fun get() = service.findAll().map(mapper::toDto)
 @PostMapping("/events") fun post(@RequestBody request: EventDto) = mapper.toDto(service.create(request))
 @PutMapping("/events/{id}") fun put(@RequestBody request: EventDto, @PathVariable id: Long): EventDto { request.id=id; return mapper.toDto(service.update(request)) }
 @DeleteMapping("/events/{id}") fun delete(@PathVariable id: Long) = service.delete(id)
}
