package com.gloomstone.clockin.worklog.mapper

import com.gloomstone.clockin.worklog.domain.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class WorklogMapperTest {
    private val mapper = WorklogMapper()

    @Test
    fun `status balance retains hours and minutes`() {
        val response = mapper.toDto(
            Status(
                isCheckedIn = false,
                gross = Duration.ofHours(7).plusMinutes(15),
                balance = Duration.ofHours(-1).minusMinutes(30),
            )
        )

        assertThat(response.gross).isEqualTo("7h 15m")
        assertThat(response.balance).isEqualTo("-1h 30m")
    }
}
