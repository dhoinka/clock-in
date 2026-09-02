package com.gloomstone.clockin.worklog.util

import java.time.Duration
import kotlin.math.absoluteValue

fun formatDuration(duration: Duration): String {
    val hours = duration.toHours().absoluteValue
    val minutes = (duration.toMinutes().absoluteValue % 60)

    val formattedDuration = StringBuilder()
    if (duration.isNegative) {
        formattedDuration.append("-")
    }
    if (hours > 0) {
        formattedDuration.append("${hours}h ")
    }
    if (minutes > 0 || formattedDuration.isEmpty()) {
        formattedDuration.append("${minutes}m")
    }

    return formattedDuration.toString().trim()
}