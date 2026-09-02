package com.gloomstone.clockin.worklog.dto

data class SettingResponse(
    var workingHours: String,
    var breakTime: String,
    var workingDays: Long,
)