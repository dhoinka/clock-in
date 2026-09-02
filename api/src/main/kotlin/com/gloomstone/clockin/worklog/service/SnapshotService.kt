package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.worklog.domain.Snapshot
import com.gloomstone.clockin.worklog.repository.DayRepository
import com.gloomstone.clockin.worklog.repository.SnapshotRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
class SnapshotService(
    private val snapshotRepository: SnapshotRepository,
    private val dayRepository: DayRepository
) {
    @Transactional
    fun createSnapshot(date: LocalDate, user: User) {
        val lastMonth = date.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth())

        val snapshot = snapshotRepository.findByUser(user) ?: Snapshot(user = user)

        if (snapshot.workday != null && date.isBefore(snapshot.workday!!.date)) {
            snapshot.workday = null
            snapshotRepository.save(snapshot)
            return
        }

        val days =
            this.dayRepository.findAllByDateLessThanEqualAndUserOrderByDate(lastMonth, user)

        if (days.isNotEmpty()) {
            val day = days.last()

            snapshot.workday.let {
                snapshot.workday = day
                snapshotRepository.save(snapshot)
                return
            }
        }
    }

    @Transactional
    fun findOrCreate(user: User): Snapshot {
        return snapshotRepository.findByUser(user) ?: snapshotRepository.save(Snapshot(user = user))
    }

    @Transactional
    fun delete(snapshot: Snapshot) {
        snapshot.workday = null
        this.snapshotRepository.save(snapshot)
    }

    @Transactional
    fun delete(user: User) {
        val snapshot = this.findOrCreate(user)
        this.delete(snapshot)
    }


}
