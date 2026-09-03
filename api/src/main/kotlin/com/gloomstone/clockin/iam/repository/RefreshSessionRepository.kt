package com.gloomstone.clockin.iam.repository

import com.gloomstone.clockin.iam.domain.RefreshSession
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface RefreshSessionRepository : JpaRepository<RefreshSession, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshSession session where session.id = :id")
    fun findByIdForUpdate(@Param("id") id: String): RefreshSession?

    fun deleteByUpdatedAtBefore(cutoff: Instant): Long
}
