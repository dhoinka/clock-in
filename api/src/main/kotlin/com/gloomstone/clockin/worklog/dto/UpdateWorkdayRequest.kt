package com.gloomstone.clockin.worklog.dto

import java.time.LocalDate

data class UpdateWorkdayRequest(
    var date: LocalDate,
    var entries: List<TimeEntryResponse> = emptyList()
)
