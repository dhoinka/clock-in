package com.gloomstone.clockin.iam.service

import com.gloomstone.clockin.iam.domain.RefreshSession
import com.gloomstone.clockin.iam.repository.RefreshSessionRepository
import com.gloomstone.clockin.shared.security.JwtUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

class RefreshSessionServiceTests {
    private val repository: RefreshSessionRepository = mock()
    private val jwtUtil = JwtUtil("test-secret")
    private val service = RefreshSessionService(repository, jwtUtil)

    @Test
    fun `rotating a token replaces it and replay revokes the session`() {
        val token = service.create("user-id", "alice")
        val sessionCaptor = argumentCaptor<RefreshSession>()
        verify(repository).save(sessionCaptor.capture())
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
    fun `cleanup deletes sessions not updated in thirty days`() {
        whenever(repository.deleteByUpdatedAtBefore(any())).thenReturn(2)

        service.deleteOldSessions()

        val cutoff = argumentCaptor<Instant>()
        verify(repository).deleteByUpdatedAtBefore(cutoff.capture())
        assertThat(cutoff.firstValue).isBefore(Instant.now().minusSeconds(29L * 24 * 60 * 60))
    }
}
