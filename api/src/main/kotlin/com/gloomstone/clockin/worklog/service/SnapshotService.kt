package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.domain.Snapshot
import com.gloomstone.clockin.worklog.domain.Workday
import com.gloomstone.clockin.worklog.repository.SnapshotRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SnapshotService(
    private val repository: SnapshotRepository,
) {
    @Transactional
    fun get(): Snapshot {
        return repository.findById(1).orElseGet { repository.save(Snapshot()) }
    }

    @Transactional
    fun invalidateFrom(date: LocalDate) {
        val snapshot = get()
        val snapshotWorkday = snapshot.workday
        if (snapshotWorkday != null && !date.isAfter(snapshotWorkday.date)) {
            snapshot.workday = null
            repository.save(snapshot)
        }
    }

    @Transactional
    fun advanceTo(workday: Workday) {
        val snapshot = get()
        if (snapshot.workday?.date?.let { !workday.date.isAfter(it) } == true) {
            return
        }

        snapshot.workday = workday
        repository.save(snapshot)
    }

    @Transactional
    fun delete() {
        val snapshot = get()
        snapshot.workday = null
        repository.save(snapshot)
    }
}
