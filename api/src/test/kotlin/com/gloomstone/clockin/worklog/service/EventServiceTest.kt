package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.shared.exception.NotFoundException
import com.gloomstone.clockin.worklog.domain.Event
import com.gloomstone.clockin.worklog.domain.EventStatus
import com.gloomstone.clockin.worklog.domain.EventType
import com.gloomstone.clockin.worklog.dto.EventDto
import com.gloomstone.clockin.worklog.mapper.EventMapper
import com.gloomstone.clockin.worklog.repository.EventRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDate
import java.util.*

class EventServiceTest {
    private val repository = mock<EventRepository>()
    private val snapshotService = mock<SnapshotService>()
    private val holidayService = mock<HolidayService>()
    private val service = EventService(repository, EventMapper(), snapshotService, holidayService)

    @BeforeEach
    fun setUp() {
        whenever(repository.save<Event>(any())).thenAnswer { it.getArgument<Event>(0) }
    }

    @Test
    fun `finds events overlapping either boundary of an inclusive range`() {
        val from = date()
        val to = date().plusDays(10)
        val startsAtFrom = event(start = from, end = from.plusDays(2))
        val endsAtTo = event(start = to.minusDays(2), end = to)
        whenever(repository.findAllOverlappingRange(from, to)).thenReturn(listOf(startsAtFrom, endsAtTo))

        assertThat(service.findAll(from, to)).containsExactly(startsAtFrom, endsAtTo)
        verify(holidayService).ensureYearLoaded(from.year)
        verify(repository).findAllOverlappingRange(from, to)
    }

    @Test
    fun `findByDate uses an inclusive one-day range`() {
        val event = event(start = date().minusDays(2), end = date().plusDays(2))
        whenever(repository.findAllOverlappingRange(date(), date())).thenReturn(listOf(event))

        assertThat(service.findByDate(date())).containsExactly(event)
    }

    @Test
    fun `range lookup synchronizes every covered year`() {
        val from = LocalDate.of(2026, 12, 28)
        val to = LocalDate.of(2027, 1, 8)
        whenever(repository.findAllOverlappingRange(from, to)).thenReturn(emptyList())

        service.findAll(from, to)

        verify(holidayService).ensureYearLoaded(2026)
        verify(holidayService).ensureYearLoaded(2027)
    }

    @Test
    fun `rejects incomplete events, invalid types, and reverse date ranges`() {
        assertThatThrownBy { service.create(EventDto(type = "vacation", start = date(), end = date())) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessage("Event title is required")
        assertThatThrownBy { service.create(EventDto(title = "Vacation", start = date(), end = date())) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessage("Event type is required")
        assertThatThrownBy {
            service.create(EventDto(title = "Vacation", type = "invalid", start = date(), end = date()))
        }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessage("Invalid event type")
        assertThatThrownBy {
            service.create(EventDto(title = "Vacation", type = "vacation", start = date().plusDays(1), end = date()))
        }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessage("Event end date must not be before the start date")
        verify(repository, never()).save<Event>(any())
    }

    @Test
    fun `create ignores client-controlled fields and invalidates the balance snapshot`() {
        val result = service.create(request(id = 99).copy(status = "approved", allDay = false))

        assertThat(result)
            .extracting("id", "status", "isAllDay")
            .containsExactly(null, EventStatus.NEW, true)
        verify(snapshotService).delete()
    }

    @Test
    fun `update persists editable fields, preserves status, and invalidates the snapshot`() {
        val existing = event(id = 7).apply { status = EventStatus.APPROVED }
        whenever(repository.findById(7)).thenReturn(Optional.of(existing))
        val request = EventDto(
            id = 7,
            title = "Sick leave",
            type = "sick",
            start = date().plusDays(10),
            end = date().plusDays(12),
            status = "new",
            allDay = false,
        )

        val result = service.update(request)

        assertThat(result)
            .extracting("title", "type", "start", "end", "status", "isAllDay")
            .containsExactly(
                "Sick leave",
                EventType.SICK,
                date().plusDays(10),
                date().plusDays(12),
                EventStatus.APPROVED,
                true,
            )
        verify(snapshotService).delete()
    }

    @Test
    fun `missing events use a typed not-found error`() {
        whenever(repository.findById(8)).thenReturn(Optional.empty())

        assertThatThrownBy { service.delete(8) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage("Event not found")
        verify(snapshotService, never()).delete()
    }

    @Test
    fun `system holiday events cannot be updated or deleted`() {
        val holiday = event(id = 9).apply { type = EventType.HOLIDAY }
        whenever(repository.findById(9)).thenReturn(Optional.of(holiday))

        assertThatThrownBy { service.update(request(id = 9)) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessage("Holiday events are read-only")
        assertThatThrownBy { service.delete(9) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessage("Holiday events are read-only")

        verify(repository, never()).delete(any())
    }

    private fun request(id: Long? = null) = EventDto(
        id = id,
        title = "Vacation",
        type = "vacation",
        start = date(),
        end = date().plusDays(2),
    )

    private fun event(
        id: Long? = null,
        start: LocalDate = date(),
        end: LocalDate = date().plusDays(2),
    ) = Event(
        title = "Vacation",
        type = EventType.VACATION,
        start = start,
        end = end,
        status = EventStatus.NEW,
        id = id,
    )

    private fun date() = LocalDate.of(2026, 9, 10)
}
