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
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EventService(
    private val repository: EventRepository,
    private val mapper: EventMapper,
    private val snapshotService: SnapshotService,
) {
    fun findAll(): List<Event> {
      return repository.findAllByOrderByStart()
    }

    fun findAll(from: LocalDate, to: LocalDate): List<Event> {
        validateRange(from, to)
        return repository.findAllOverlappingRange(from, to)
    }

    @Transactional
    @CacheEvict(value = ["events"], allEntries = true)
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
    @CacheEvict(value = ["events"], allEntries = true)
    fun update(request: EventDto): Event {
        val id = request.id
            ?: throw BadRequestException("Event id is required")

        validateEvent(request)
        val event = repository.findById(id).orElseThrow { NotFoundException("Event not found") }
        event.title = request.title
        event.type = eventType(requireNotNull(request.type))
        event.start = request.start
        event.end = request.end
        event.isAllDay = true
        return repository.save(event).also { snapshotService.delete() }
    }

    @Transactional
    @CacheEvict(value = ["events"], allEntries = true)
    fun delete(id: Long) {
        val event = repository.findById(id).orElseThrow { NotFoundException("Event not found") }
        repository.delete(event)
        snapshotService.delete()
    }

    @Cacheable(value = ["events"], key = "#date")
    fun findByDate(date: LocalDate): List<Event> = repository.findAllOverlappingRange(date, date)

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
        EventType.entries.firstOrNull { it.value == value }
            ?: throw BadRequestException("Invalid event type")
}
