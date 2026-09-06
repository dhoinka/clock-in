package com.gloomstone.clockin.iam.service

import com.gloomstone.clockin.config.AppConfig
import com.gloomstone.clockin.iam.domain.RefreshSession
import com.gloomstone.clockin.iam.repository.RefreshSessionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

class SessionServiceTests {
    private val repository: RefreshSessionRepository = mock()
    private val service = SessionService(repository, AppConfig())

    @Test
    fun `rotating a token replaces it and replay revokes the session`() {
        val token = service.create("user-id")
        val sessionCaptor = argumentCaptor<RefreshSession>()
        verify(repository).saveAndFlush(sessionCaptor.capture())
        val session = sessionCaptor.firstValue
        whenever(repository.findByIdForUpdate(session.id)).thenReturn(session)

        val rotation = service.rotate(token)

        assertThat(rotation).isNotNull
        assertThat(rotation?.refreshToken).isNotEqualTo(token)
        assertThat(session.revokedAt).isNull()

        val replay = service.rotate(token)

        assertThat(replay).isNull()
        assertThat(session.revokedAt).isNotNull()
    }

    @Test
    fun `replaying a token older than the previous rotation still revokes the session`() {
        val originalToken = service.create("user-id")
        val sessionCaptor = argumentCaptor<RefreshSession>()
        verify(repository).saveAndFlush(sessionCaptor.capture())
        val session = sessionCaptor.firstValue
        whenever(repository.findByIdForUpdate(session.id)).thenReturn(session)

        val firstRotation = service.rotate(originalToken) ?: error("First rotation failed")
        val secondRotation = service.rotate(firstRotation.refreshToken)

        assertThat(secondRotation).isNotNull
        assertThat(service.rotate(originalToken)).isNull()
        assertThat(session.revokedAt).isNotNull()
    }

    @Test
    fun `cleanup deletes sessions not updated in thirty days`() {
        whenever(repository.deleteByUpdatedAtBefore(any())).thenReturn(2)

        service.deleteOldSessions()

        val cutoff = argumentCaptor<Instant>()
        verify(repository).deleteByUpdatedAtBefore(cutoff.capture())
        assertThat(cutoff.firstValue).isBefore(Instant.now().minusSeconds(29L * 24 * 60 * 60))
    }
}
