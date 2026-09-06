package com.gloomstone.clockin.iam.service

import com.gloomstone.clockin.config.AppConfig
import com.gloomstone.clockin.iam.domain.RefreshSession
import com.gloomstone.clockin.iam.repository.RefreshSessionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class SessionService(
    private val repository: RefreshSessionRepository,
    private val appConfig: AppConfig,
) {
    data class Rotation(val userId: String, val refreshToken: String)

    private val logger = LoggerFactory.getLogger(SessionService::class.java)

    @Transactional()
    fun create(userId: String): String {
        val now = Instant.now()
        val sessionId = UUID.randomUUID().toString()
        val token = newToken(sessionId)
        repository.saveAndFlush(
            RefreshSession(
                id = sessionId,
                userId = userId,
                tokenHash = hash(token),
                expiresAt = now.plus(appConfig.refreshTokenLifetime),
                createdAt = now,
                updatedAt = now,
            )
        )
        logger.info("Refresh session created, ref={}", sessionRef(sessionId))
        return token
    }

    @Transactional()
    fun rotate(rawToken: String): Rotation? {
        val sessionId = sessionId(rawToken) ?: run {
            logger.warn("Refresh rejected: malformed refresh credential")
            return null
        }
        val tokenHash = hash(rawToken)
        val session = repository.findByIdForUpdate(sessionId) ?: run {
            logger.warn("Refresh rejected: session does not exist, ref={}", sessionRef(sessionId))
            return null
        }
        val now = Instant.now()

        val matchesCurrentToken = MessageDigest.isEqual(
            session.tokenHash.toByteArray(StandardCharsets.US_ASCII),
            tokenHash.toByteArray(StandardCharsets.US_ASCII),
        )

        if (!matchesCurrentToken) {
            logger.warn("Refresh rejected: credential replay or mismatch; revoking session")
            session.revokedAt = now
            session.updatedAt = now
            repository.save(session)
            return null
        }
        if (session.revokedAt != null) {
            logger.warn("Refresh rejected: session is already revoked")
            return null
        }
        if (!session.expiresAt.isAfter(now)) {
            logger.warn("Refresh rejected: session has expired; revoking session")
            session.revokedAt = now
            session.updatedAt = now
            repository.save(session)
            return null
        }

        val replacement = newToken(session.id)
        session.tokenHash = hash(replacement)
        // Rotation changes the credential but does not turn a session into an indefinitely sliding login.
        session.expiresAt = session.createdAt.plus(appConfig.refreshTokenLifetime)
        session.updatedAt = now
        repository.save(session)
        logger.info("Refresh session rotated, ref={}", sessionRef(session.id))
        return Rotation(session.userId, replacement)
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun deleteOldSessions() {
        val deleted = repository.deleteByUpdatedAtBefore(Instant.now().minus(30, ChronoUnit.DAYS))
        if (deleted > 0) {
            logger.info("Deleted {} old refresh sessions", deleted)
        }
    }

    @Transactional()
    fun revoke(rawToken: String?) {
        if (rawToken == null) return
        val sessionId = sessionId(rawToken) ?: return
        val tokenHash = hash(rawToken)
        val session = repository.findByIdForUpdate(sessionId) ?: return
        val matchesCurrentToken = MessageDigest.isEqual(
            session.tokenHash.toByteArray(StandardCharsets.US_ASCII),
            tokenHash.toByteArray(StandardCharsets.US_ASCII),
        )
        if (matchesCurrentToken && session.revokedAt == null) {
            val now = Instant.now()
            session.revokedAt = now
            session.updatedAt = now
            repository.save(session)
        }
    }

    private fun hash(token: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(StandardCharsets.UTF_8))
    )

    private fun newToken(sessionId: String): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return "$sessionId.$secret"
    }

    private fun sessionId(token: String): String? {
        val separator = token.indexOf('.')
        if (separator <= 0 || separator == token.lastIndex) return null
        return token.substring(0, separator)
            .takeIf { runCatching { UUID.fromString(it) }.isSuccess }
    }

    private fun sessionRef(sessionId: String): String = sessionId.take(8)
}
