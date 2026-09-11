package com.gloomstone.clockin.worklog.util

import java.time.Duration

internal val REGEX_DURATION = """
    ^-?(?=.*\d+(?:ms|[dhms]))
    ((?<days>\d+)d)?(\s*)
    ((?<hours>\d+)h)?(\s*)
    ((?<minutes>\d+)m)?(\s*)
    ((?<seconds>\d+)s)?(\s*)
    ((?<milliseconds>\d+)ms)?$
""".replace("\\s+".toRegex(), "")
    .trim()
    .toRegex()

internal const val MS = 1L
internal const val SEC = 1000 * MS
internal const val MIN = 60 * SEC
internal const val HOUR = 60 * MIN
internal const val DAY = 24 * HOUR

internal val GROUP_UNIT_CONVERTERS = mapOf(
    "days" to DAY,
    "hours" to HOUR,
    "minutes" to MIN,
    "seconds" to SEC,
    "milliseconds" to MS,
)

fun String.toDuration(): Duration? {
    val input = trim()
    val matcher = REGEX_DURATION.find(input) ?: return null
    var durationMs = 0L

    GROUP_UNIT_CONVERTERS.forEach { (groupName, multiplier) ->
        val amount = (matcher.groups as MatchNamedGroupCollection)[groupName]?.value?.toLong()
        if (amount != null) {
            durationMs += amount * multiplier
        }
    }

    return if (input.startsWith("-")) {
        Duration.ofMillis(-durationMs)
    } else {
        Duration.ofMillis(durationMs)
    }
}
