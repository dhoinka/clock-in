package com.gloomstone.clockin.worklog.service

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
) {
    fun findAll(): List<Event> = repository.findAllByOrderByStart()

    @Transactional
    @CacheEvict(value = ["events"], allEntries = true)
    fun create(request: EventDto): Event {
        if (request.status == null) request.status = EventStatus.NEW.value
        return repository.save(mapper.toEntity(request))
    }

    @Transactional
    @CacheEvict(value = ["events"], allEntries = true)
    fun update(request: EventDto): Event {
        val event = repository.findById(requireNotNull(request.id)).orElseThrow()
        event.title = request.title
        event.start = request.start
        event.end = request.end
        event.type = request.type?.let(EventType::fromValue)
        event.status = request.status?.let(EventStatus::fromValue)
        event.isAllDay = request.isAllDay
        return repository.save(event)
    }

    @Transactional
    @CacheEvict(value = ["events"], allEntries = true)
    fun delete(id: Long) = repository.deleteById(id)

    @Cacheable(value = ["events"], key = "#date")
    fun findByDate(date: LocalDate): List<Event> =
        repository.findAllByStartGreaterThanEqualAndEndLessThanEqual(date, date)
}
