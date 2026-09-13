package com.gloomstone.clockin.worklog.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class DurationParseTest {
    @Test
    fun `parses a trimmed negative duration`() {
        assertThat("  -1h 30m  ".toDuration()).isEqualTo(Duration.ofMinutes(-90))
    }

    @Test
    fun `rejects values without a valid duration component`() {
        assertThat("".toDuration()).isNull()
        assertThat("-".toDuration()).isNull()
        assertThat("---1h".toDuration()).isNull()
    }
}
