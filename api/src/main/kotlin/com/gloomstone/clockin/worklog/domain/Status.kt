package com.gloomstone.clockin.worklog.domain

import java.time.Duration

data class Status(var isCheckedIn: Boolean?, var gross: Duration?, var balance: Duration?)
