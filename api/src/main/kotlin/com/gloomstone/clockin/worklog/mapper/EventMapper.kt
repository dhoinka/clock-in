package com.gloomstone.clockin.worklog.mapper
import com.gloomstone.clockin.worklog.domain.Event
import com.gloomstone.clockin.worklog.domain.EventStatus
import com.gloomstone.clockin.worklog.domain.EventType
import com.gloomstone.clockin.worklog.dto.EventDto
import org.springframework.stereotype.Component

@Component
class EventMapper {
    fun toDto(event: Event) = EventDto(
        id = event.id,
        title = event.title,
        type = event.type?.value.orEmpty(),
        start = event.start,
        end = event.end,
        status = event.status?.value.orEmpty(),
        username = event.user?.username,
    )

    fun toEntity(request: EventDto) = Event(
        id = request.id,
        title = request.title,
        type = request.type?.let(EventType::fromValue),
        start = request.start,
        end = request.end,
        status = request.status?.let(EventStatus::fromValue),
    )
}
