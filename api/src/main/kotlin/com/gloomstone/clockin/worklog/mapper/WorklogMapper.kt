package com.gloomstone.clockin.worklog.mapper

import com.gloomstone.clockin.worklog.domain.Status
import com.gloomstone.clockin.worklog.domain.TimeEntry
import com.gloomstone.clockin.worklog.domain.Workday
import com.gloomstone.clockin.worklog.dto.StatusResponse
import com.gloomstone.clockin.worklog.dto.TimeEntryResponse
import com.gloomstone.clockin.worklog.dto.WorkdayResponse
import com.gloomstone.clockin.worklog.util.formatDuration
import org.springframework.stereotype.Component

@Component
class WorklogMapper {
    fun toDto(status: Status) = StatusResponse(
        isCheckedIn = status.isCheckedIn,
        gross = formatDuration(status.gross),
        balance = formatDuration(status.balance),
    )

    fun toDto(timeEntry: TimeEntry) = TimeEntryResponse(
        type = timeEntry.type.value,
        start = timeEntry.start,
        end = timeEntry.end,
        duration = timeEntry.duration,
        date = timeEntry.workday?.date,
        id = timeEntry.id,
    )

    fun toDto(workday: Workday) = WorkdayResponse(
        date = workday.date.toString(),
        entries = workday.entries.map(::toDto),
        gross = workday.gross?.let(::formatDuration),
        balance = workday.balance?.let(::formatDuration),
        isWorkday = workday.isWorkday,
    )
}
