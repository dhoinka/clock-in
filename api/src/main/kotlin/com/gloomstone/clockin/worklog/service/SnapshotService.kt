package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.domain.Snapshot
import com.gloomstone.clockin.worklog.repository.DayRepository
import com.gloomstone.clockin.worklog.repository.SnapshotRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
class SnapshotService(
    private val repository: SnapshotRepository,
    private val days: DayRepository,
) {
    @Transactional
    fun get(): Snapshot = repository.findById(1).orElseGet { repository.save(Snapshot()) }

    @Transactional
    fun createSnapshot(date: LocalDate) {
        val snapshot = get()
        if (snapshot.workday != null && date.isBefore(requireNotNull(snapshot.workday).date)) {
            snapshot.workday = null
            repository.save(snapshot)
            return
        }

        val previousMonth = date.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
        days.findAllByDateLessThanEqualOrderByDate(previousMonth).lastOrNull()?.let {
            snapshot.workday = it
            repository.save(snapshot)
        }
    }

    @Transactional
    fun delete() {
        val snapshot = get()
        snapshot.workday = null
        repository.save(snapshot)
    }
}
