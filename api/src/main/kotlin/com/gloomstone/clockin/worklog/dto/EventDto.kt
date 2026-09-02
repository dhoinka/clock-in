package com.gloomstone.clockin.worklog.dto

import java.time.LocalDate

data class EventDto(
    var id: Long? = null,
    var title: String? = null,
    var type: String? = null,
    var start: LocalDate? = null,
    var end: LocalDate? = null,
    var status: String? = null,
    var username: String? = null,
)
