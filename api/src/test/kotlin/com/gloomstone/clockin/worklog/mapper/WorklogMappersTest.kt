package com.gloomstone.clockin.worklog.mapper

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.worklog.domain.EntryType
import com.gloomstone.clockin.worklog.domain.Event
import com.gloomstone.clockin.worklog.domain.EventStatus
import com.gloomstone.clockin.worklog.domain.EventType
import com.gloomstone.clockin.worklog.domain.Setting
import com.gloomstone.clockin.worklog.domain.TimeEntry
import com.gloomstone.clockin.worklog.domain.Workday
import com.gloomstone.clockin.worklog.dto.EventDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class WorklogMappersTest {
    private val user = User("alice@example.com", "alice", "Alice")

    @Test
    fun `maps events in both directions without assigning ownership`() {
        val mapper = EventMapper()
        val date = LocalDate.of(2026, 9, 3)

        val entity = mapper.toEntity(
            EventDto(title = "Holiday", type = "vacation", start = date, status = "approved", username = "bob")
        )

        assertThat(entity.type).isEqualTo(EventType.VACATION)
        assertThat(entity.status).isEqualTo(EventStatus.APPROVED)
        assertThat(entity.user).isNull()

        val response = mapper.toDto(Event(title = "Holiday", type = EventType.VACATION, user = user))
        assertThat(response.type).isEqualTo("vacation")
        assertThat(response.username).isEqualTo("alice")
    }

    @Test
    fun `maps a workday and its entries`() {
        val date = LocalDate.of(2026, 9, 3)
        val workday = Workday(date, user, gross = Duration.ofHours(8), balance = Duration.ofMinutes(30))
        val entry = TimeEntry(LocalDateTime.of(2026, 9, 3, 9, 0), workday, user).apply {
            id = 42
            type = EntryType.STANDARD
        }
        workday.entries += entry

        val result = WorklogMapper().toDto(workday)

        assertThat(result.date).isEqualTo("2026-09-03")
        assertThat(result.user).isEqualTo("alice")
        assertThat(result.gross).isEqualTo("8h")
        val mappedEntry = result.entries!!.single()
        assertThat(mappedEntry.id).isEqualTo(42)
        assertThat(mappedEntry.type).isEqualTo("standard")
        assertThat(mappedEntry.date).isEqualTo(date)
    }

    @Test
    fun `formats settings`() {
        val setting = Setting(Duration.ofHours(8), Duration.ofMinutes(30), 31, user)

        val result = SettingMapper().toDto(setting)

        assertThat(result.workingHours).isEqualTo("8h")
        assertThat(result.breakTime).isEqualTo("30m")
        assertThat(result.workingDays).isEqualTo(31)
    }
}
