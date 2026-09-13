package com.gloomstone.clockin.worklog.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class DurationFormatTest {
    @Test
    fun `formats whole hours`() {
        assertThat(formatDuration(Duration.ofHours(8))).isEqualTo("8h")
    }

    @Test
    fun `formats whole minutes`() {
        assertThat(formatDuration(Duration.ofMinutes(30))).isEqualTo("30m")
    }

    @Test
    fun `formats hours and minutes`() {
        assertThat(formatDuration(Duration.ofHours(8).plusMinutes(30))).isEqualTo("8h 30m")
    }
}
