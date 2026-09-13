package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.domain.Event
import com.gloomstone.clockin.worklog.domain.EventStatus
import com.gloomstone.clockin.worklog.domain.EventType
import com.gloomstone.clockin.worklog.dto.EventDto
import com.gloomstone.clockin.worklog.mapper.EventMapper
import com.gloomstone.clockin.worklog.repository.EventRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

class EventServiceTest {
    private val repository: EventRepository = mock()
    private val mapper: EventMapper = mock()
    private val service = EventService(repository, mapper)

    @Test
    fun `creates a global event with a default status`() {
        val request = EventDto(title = "Holiday", type = "VACATION")
        val event = Event(title = "Holiday", type = EventType.VACATION, status = EventStatus.NEW)
        whenever(mapper.toEntity(request)).thenReturn(event)
        whenever(repository.save(event)).thenReturn(event)

        assertThat(service.create(request)).isSameAs(event)
        assertThat(request.status).isEqualTo("new")
        verify(repository).save(event)
    }

    @Test
    fun `updates the shared event without a username`() {
        val existing = Event(title = "Old", id = 5)
        whenever(repository.findById(5)).thenReturn(Optional.of(existing))
        whenever(repository.save(existing)).thenReturn(existing)
        val request = EventDto(id = 5, title = "New", type = "vacation", status = "approved", start = LocalDate.of(2026, 9, 1))

        val updated = service.update(request)

        assertThat(updated.title).isEqualTo("New")
        assertThat(updated.type).isEqualTo(EventType.VACATION)
        assertThat(updated.status).isEqualTo(EventStatus.APPROVED)
    }
}
