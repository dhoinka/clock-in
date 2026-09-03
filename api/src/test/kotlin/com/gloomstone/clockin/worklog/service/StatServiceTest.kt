package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.repository.UserRepository
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.exception.InternalServerException
import com.gloomstone.clockin.worklog.dto.StatMapping
import com.gloomstone.clockin.worklog.repository.StatRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class StatServiceTest {
    private val timeEntryRepository = mock<TimeEntryRepository>()
    private val userRepository = mock<UserRepository>()
    private val statRepository = mock<StatRepository>()
    private val userService = mock<UserService>()
    private val service = StatService(timeEntryRepository, userRepository, statRepository, userService)

    @Test
    fun `getStats maps decimal database values`() {
        val user = user("daniel")
        whenever(userService.findByIdentity("daniel")).thenReturn(user)
        whenever(timeEntryRepository.getStats(user.id)).thenReturn(
            mapOf(
                "username" to "daniel",
                "start_ts" to "8.36638418079096045198",
                "end_ts" to "17.3209039548022598870",
            )
        )

        val stats = service.getStats("daniel")

        assertThat(stats.username).isEqualTo("daniel")
        assertThat(stats.avgStart).isEqualTo(8.366384180790961)
        assertThat(stats.avgEnd).isEqualTo(17.32090395480226)
        verify(timeEntryRepository).getStats(user.id)
    }

    @Test
    fun `getStats returns zero averages when there are no completed entries`() {
        val user = user("no-entries")
        whenever(userService.findByIdentity(user.username)).thenReturn(user)
        whenever(timeEntryRepository.getStats(user.id)).thenReturn(emptyMap())

        assertThat(service.getStats(user.username))
            .extracting("username", "avgStart", "avgEnd")
            .containsExactly(user.username, 0.0, 0.0)
    }

    @Test
    fun `getStats tolerates malformed native query results`() {
        val user = user("daniel")
        whenever(userService.findByIdentity(user.username)).thenReturn(user)
        whenever(timeEntryRepository.getStats(user.id)).thenReturn(mapOf("username" to user.username))

        assertThat(service.getStats(user.username))
            .extracting("username", "avgStart", "avgEnd")
            .containsExactly(user.username, 0.0, 0.0)
    }

    @Test
    fun `getStats rejects an unknown user`() {
        whenever(userService.findByIdentity("missing")).thenReturn(null)

        assertThatThrownBy { service.getStats("missing") }
            .isInstanceOf(InternalServerException::class.java)
            .hasMessage("User not found")
    }

    @Test
    fun `getAdminStats includes users without entries and groups entries by username`() {
        val daniel = user("daniel")
        val alice = user("alice")
        val september = StatMapping("daniel", LocalDate.of(2026, 9, 1), 4)
        val october = StatMapping("daniel", LocalDate.of(2026, 10, 1), 2)
        whenever(userRepository.findAll()).thenReturn(listOf(daniel, alice))
        whenever(statRepository.getStat()).thenReturn(listOf(september, october))

        val stats = service.getAdminStats()

        assertThat(stats["daniel"]).containsExactly(september, october)
        assertThat(stats["alice"]).isEmpty()
        verify(userRepository).findAll()
        verify(statRepository).getStat()
    }

    private fun user(username: String) = User(
        email = "$username@example.org",
        username = username,
        name = username,
        id = "$username-id",
    )
}
