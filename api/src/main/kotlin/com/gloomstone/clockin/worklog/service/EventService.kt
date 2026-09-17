package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.shared.exception.NotFoundException
import com.gloomstone.clockin.worklog.domain.Event
import com.gloomstone.clockin.worklog.domain.EventStatus
import com.gloomstone.clockin.worklog.domain.EventType
import com.gloomstone.clockin.worklog.dto.EventDto
import com.gloomstone.clockin.worklog.mapper.EventMapper
import com.gloomstone.clockin.worklog.repository.EventRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EventService(
    private val repository: EventRepository,
    private val mapper: EventMapper,
    private val snapshotService: SnapshotService,
    private val holidayService: HolidayService,
) {
    fun findAll(): List<Event> {
        return repository.findAllByOrderByStart()
    }

    fun findAll(from: LocalDate, to: LocalDate): List<Event> {
        validateRange(from, to)
        (from.year..to.year).forEach(holidayService::ensureYearLoaded)
        return repository.findAllOverlappingRange(from, to)
    }

    @Transactional
    fun create(request: EventDto): Event {
        validateEvent(request)
        val event = mapper.toEntity(request).apply {
            id = null
            status = EventStatus.NEW
            isAllDay = true
        }
        return repository.save(event).also { snapshotService.delete() }
    }

    @Transactional
    fun update(request: EventDto): Event {
        val id = request.id
            ?: throw BadRequestException("Event id is required")

        validateEvent(request)
        val event = repository.findById(id).orElseThrow { NotFoundException("Event not found") }
        rejectHolidayMutation(event)
        event.title = request.title
        event.type = eventType(requireNotNull(request.type))
        event.start = request.start
        event.end = request.end
        event.isAllDay = true
        return repository.save(event).also { snapshotService.delete() }
    }

    @Transactional
    fun delete(id: Long) {
        val event = repository.findById(id).orElseThrow { NotFoundException("Event not found") }
        rejectHolidayMutation(event)
        repository.delete(event)
        snapshotService.delete()
    }

    fun findByDate(date: LocalDate): List<Event> = repository.findAllOverlappingRange(date, date)

    private fun rejectHolidayMutation(event: Event) {
        if (event.type == EventType.HOLIDAY) {
            throw BadRequestException("Holiday events are read-only")
        }
    }

    private fun validateEvent(request: EventDto) {
        if (request.title.isNullOrBlank()) {
            throw BadRequestException("Event title is required")
        }
        val type = request.type
        if (type.isNullOrBlank()) {
            throw BadRequestException("Event type is required")
        }
        eventType(type)

        val start = request.start ?: throw BadRequestException("Event start date is required")
        val end = request.end ?: throw BadRequestException("Event end date is required")
        validateRange(start, end)
    }

    private fun validateRange(from: LocalDate, to: LocalDate) {
        if (to.isBefore(from)) {
            throw BadRequestException("Event end date must not be before the start date")
        }
    }

    private fun eventType(value: String): EventType =
        EventType.entries.firstOrNull { it != EventType.HOLIDAY && it.value == value }
            ?: throw BadRequestException("Invalid event type")
}
