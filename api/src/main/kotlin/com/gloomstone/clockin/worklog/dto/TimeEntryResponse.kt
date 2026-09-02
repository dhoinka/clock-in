package com.gloomstone.clockin.worklog.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class TimeEntryResponse(
    var type: String,
    var start: LocalDateTime? = null,
    var end: LocalDateTime? = null,
    var duration: String? = null,
    var date: LocalDate? = null,
    var id: Long? = null,
)
