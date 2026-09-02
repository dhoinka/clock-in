package com.gloomstone.clockin.worklog.dto

data class WorkdayResponse(
    var date: String? = null,
    var entries: List<TimeEntryResponse>? = null,
    var gross: String? = null,
    var balance: String? = null,
    var isWorkday: Boolean = false,
    var user: String? = null,
)