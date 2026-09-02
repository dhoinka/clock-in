package com.gloomstone.clockin.worklog.repository

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.worklog.domain.Snapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SnapshotRepository : JpaRepository<Snapshot, Long> {
    fun findByUser(user: User): Snapshot?
}
