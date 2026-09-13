package com.gloomstone.clockin.worklog.domain

import java.time.Duration

data class Status(
    val isCheckedIn: Boolean,
    val gross: Duration,
    val balance: Duration,
)
