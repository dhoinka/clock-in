package com.gloomstone.clockin.worklog.mapper


import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.worklog.domain.Event
import com.gloomstone.clockin.worklog.domain.EventStatus
import com.gloomstone.clockin.worklog.domain.EventType
import com.gloomstone.clockin.worklog.dto.EventDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface EventMapper {
    @Mapping(target = "username", source = "user")
    fun toDto(event: Event): EventDto

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "allDay", ignore = true)
    fun toEntity(request: EventDto): Event

    fun mapToString(user: User): String {
        return user.username
    }

    fun mapToEventType(value: String): EventType {
        return EventType.fromValue(value)
    }

    fun mapToEventStatus(value: String): EventStatus {
        return EventStatus.fromValue(value)
    }


    fun mapToString(type: EventType?): String {
        if (type == null) {
            return ""
        }
        return type.value
    }

    fun mapToString(status: EventStatus?): String {
        if (status == null) {
            return ""
        }
        return status.value
    }
}
