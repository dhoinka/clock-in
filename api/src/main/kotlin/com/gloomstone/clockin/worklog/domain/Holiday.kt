package com.gloomstone.clockin.worklog.domain

import java.time.LocalDate

data class Holiday(
    val date: LocalDate,
    val name: String,
    var isAllStates: Boolean = false,
    var isBw: Boolean = false,
    var isBy: Boolean = false,
    var isBe: Boolean = false,
    var isBb: Boolean = false,
    var isHb: Boolean = false,
    var isHh: Boolean = false,
    var isHe: Boolean = false,
    var isMv: Boolean = false,
    var isNi: Boolean = false,
    var isNw: Boolean = false,
    var isRp: Boolean = false,
    var isSl: Boolean = false,
    var isSn: Boolean = false,
    var isSt: Boolean = false,
    var isSh: Boolean = false,
    var isTh: Boolean = false
)
