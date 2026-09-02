package com.gloomstone.clockin.worklog.dto

import java.time.LocalDate

data class StatMapping(var username: String, var month: LocalDate, var count: Long)