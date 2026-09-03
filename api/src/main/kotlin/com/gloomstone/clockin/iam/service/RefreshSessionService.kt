package com.gloomstone.clockin.iam.service

import com.gloomstone.clockin.iam.domain.RefreshSession
import com.gloomstone.clockin.iam.repository.RefreshSessionRepository
import com.gloomstone.clockin.shared.security.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Propagation
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.HexFormat
import java.util.UUID

@Service
class RefreshSessionService(
    private val repository: RefreshSessionRepository,
    private val jwtUtil: JwtUtil,
) {
    data class Rotation(val userId: String, val refreshToken: String)

    private val logger = LoggerFactory.getLogger(RefreshSessionService::class.java)

    @Transactional
    fun create(userId: String, username: String): String {
        val now = Instant.now()
        val sessionId = UUID.randomUUID().toString()
        val token = jwtUtil.generateRefreshToken(username, sessionId)
        repository.save(
            RefreshSession(
                id = sessionId,
                userId = userId,
                tokenJti = token.jti,
                tokenHash = hash(token.value),
                expiresAt = token.expiresAt,
                createdAt = now,
                updatedAt = now,
            )
        )
        return token.value
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun rotate(rawToken: String): Rotation? {
        val claims = jwtUtil.verifyRefreshToken(rawToken)
        val session = repository.findByIdForUpdate(claims.sessionId) ?: return null
        val now = Instant.now()

        val matchesCurrentToken = session.tokenJti == claims.jti &&
            MessageDigest.isEqual(
                session.tokenHash.toByteArray(StandardCharsets.US_ASCII),
                hash(rawToken).toByteArray(StandardCharsets.US_ASCII),
            )

        if (!matchesCurrentToken || session.revokedAt != null || !session.expiresAt.isAfter(now)) {
            session.revokedAt = now
            session.updatedAt = now
            repository.save(session)
            return null
        }

        val replacement = jwtUtil.generateRefreshToken(claims.username, session.id)
        session.tokenJti = replacement.jti
        session.tokenHash = hash(replacement.value)
        session.expiresAt = replacement.expiresAt
        session.updatedAt = now
        repository.save(session)
        return Rotation(session.userId, replacement.value)
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun deleteOldSessions() {
        val deleted = repository.deleteByUpdatedAtBefore(Instant.now().minus(30, ChronoUnit.DAYS))
        if (deleted > 0) {
            logger.info("Deleted {} old refresh sessions", deleted)
        }
    }

    private fun hash(token: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(StandardCharsets.UTF_8))
    )
}
