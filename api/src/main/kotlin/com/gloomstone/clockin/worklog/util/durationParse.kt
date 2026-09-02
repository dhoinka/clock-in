package com.gloomstone.clockin.worklog.util

import java.time.Duration

internal val REGEX_DURATION = """
    ^-*(
       (
         ((?<days>\d+)d)?(\s*)
         ((?<hours>\d+)h)?(\s*)
         ((?<minutes>\d+)m)?(\s*)
         ((?<seconds>\d+)s)?(\s*)
         ((?<milliseconds>\d+)ms)?
      )
    | (
         ((?<days2>\d+)d)
        |((?<hours2>\d+)h)
        |((?<minutes2>\d+)m)
        |((?<seconds2>\d+)s)
        |((?<milliseconds2>\d+)ms)
      )
    )$
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
    "days2" to DAY,
    "hours2" to HOUR,
    "minutes2" to MIN,
    "seconds2" to SEC,
    "milliseconds2" to MS
)

fun String.toDuration(): Duration? {
    val matcher = REGEX_DURATION.find(this.trim()) ?: return null
    var durationNs = 0L

    GROUP_UNIT_CONVERTERS.forEach { (groupName, multiplier) ->
        val amount = (matcher.groups as MatchNamedGroupCollection)[groupName]?.value?.toLong()
        if (amount != null)
            durationNs += amount * multiplier
    }

    val result = if (this.startsWith("-")) {
        Duration.ofMillis(durationNs * -1)
    } else {
        Duration.ofMillis(durationNs)
    }
    return result
}
