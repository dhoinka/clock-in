package com.gloomstone.clockin.worklog.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration


class DurationFormatTest {
    @Test
    fun testFormatDuration() {
        val duration = Duration.ofHours(8)
        val result = formatDuration(duration)
        assertThat(result).isEqualTo("8h")
    }

    @Test
    fun testFormatDurationMinutes() {
        val duration = Duration.ofMinutes(30)
        val result = formatDuration(duration)
        assertThat(result).isEqualTo("30m")
    }

    @Test
    fun testFormatDurationHoursMinutes() {
        val duration = Duration.ofHours(8).plusMinutes(30)
        val result = formatDuration(duration)
        assertThat(result).isEqualTo("8h 30m")
    }
}