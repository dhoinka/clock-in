package com.gloomstone.clockin.worklog.mapper

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.worklog.domain.EntryType
import com.gloomstone.clockin.worklog.domain.Status
import com.gloomstone.clockin.worklog.domain.TimeEntry
import com.gloomstone.clockin.worklog.domain.Workday
import com.gloomstone.clockin.worklog.dto.StatusResponse
import com.gloomstone.clockin.worklog.dto.TimeEntryResponse
import com.gloomstone.clockin.worklog.dto.WorkdayResponse
import com.gloomstone.clockin.worklog.util.formatDuration
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
import org.mapstruct.NullValueCheckStrategy
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Mapper(
    componentModel = "spring",
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
interface WorklogMapper {
    @Mapping(target = "gross", qualifiedByName = ["formatTime"])
    fun toDto(status: Status): StatusResponse

    @Mapping(target = "date", source = "workday.date")
    fun toDto(timeEntry: TimeEntry): TimeEntryResponse

    @Mapping(target = "date", qualifiedByName = ["formatDate"])
    @Mapping(target = "gross", qualifiedByName = ["formatTime"])
    @Mapping(target = "balance", qualifiedByName = ["formatTime"])
    @Mapping(target = "user", source = "user")
    fun toDto(workday: Workday): WorkdayResponse

    fun map(value: User): String {
        return value.username
    }

    fun map(entryType: EntryType): String {
        return entryType.name.lowercase(Locale.getDefault())
    }

    fun toDto(value: Duration): String {
        return value.toHours().toString()
    }

    @Named("formatTime")
    fun formatTime(duration: Duration): String {
        return formatDuration(duration)
    }

    @Named("formatDate")
    fun formatDate(date: LocalDate): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(date)
    }

}
