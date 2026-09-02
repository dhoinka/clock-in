package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.worklog.domain.Event
import com.gloomstone.clockin.worklog.domain.EventStatus
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
    private val eventRepository: EventRepository,
    private val userService: UserService,
    private val mapper: EventMapper
) {
    fun findAllByUsername(username: String): List<Event> {
        val user = userService.findByIdentity(username) ?: throw BadRequestException("User not found")
        return eventRepository.findAllByUser(user)
    }

    @CacheEvict(value = ["events"], allEntries = true)
    @Transactional
    fun create(request: EventDto, username: String): Event {
        val user = this.userService.findByIdentity(username) ?: throw BadRequestException("User not found")
        if (request.status == null) {
            request.status = EventStatus.NEW.toString()
        }

        val event = mapper.toEntity(request).apply {
            this.user = user
        }

        return eventRepository.save(event)
    }

    @CacheEvict(value = ["events"], allEntries = true)
    @Transactional
    fun update(request: EventDto, username: String): Event {
        val event = eventRepository.findById(request.id!!).orElseThrow()
        val user = this.userService.findByIdentity(username) ?: throw BadRequestException("User not found")
        if (event.user?.username != user.username) {
            throw BadRequestException("Username mismatch")
        }
        event.title = request.title
        event.start = request.start
        event.end = request.end
        return eventRepository.save(event)
    }

    @CacheEvict(value = ["events"], allEntries = true)
    @Transactional
    fun delete(id: Long, username: String) {
        val event = eventRepository.findById(id).orElseThrow()
        if (event.user?.username != username) {
            throw BadRequestException("You can't delete this event")
        }
        eventRepository.delete(event)
    }

    @Cacheable(value = ["events"], key = "#username + #date")
    fun findByDateAndUsername(date: LocalDate, username: String): List<Event> {
        val user = userService.findByIdentity(username) ?: throw BadRequestException()
        return eventRepository.findAllByStartGreaterThanEqualAndEndLessThanEqualAndUser(
            date,
            date,
            user
        )
    }


}
