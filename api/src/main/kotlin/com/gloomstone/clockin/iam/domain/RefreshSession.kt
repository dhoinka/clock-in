package com.gloomstone.clockin.iam.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "refresh_session")
class RefreshSession(
    @Id
    var id: String,
    @Column(name = "user_id", nullable = false)
    var userId: String,
    @Column(name = "token_hash", nullable = false, length = 64)
    var tokenHash: String,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
)
