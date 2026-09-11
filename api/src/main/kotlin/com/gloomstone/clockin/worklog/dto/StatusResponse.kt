package com.gloomstone.clockin.worklog.dto

data class StatusResponse(
    var isCheckedIn: Boolean = false,
    var gross: String,
    var balance: String,
)
