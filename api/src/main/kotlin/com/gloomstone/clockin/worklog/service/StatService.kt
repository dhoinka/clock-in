package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.domain.Stat
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import org.springframework.stereotype.Service

@Service
class StatService(private val entries: TimeEntryRepository) {
    fun getStats(): Stat {
        val completed = entries.findAll().filter { it.start != null && it.end != null }
        if (completed.isEmpty()) return Stat()

        val starts = completed.map { requireNotNull(it.start) }
        val ends = completed.map { requireNotNull(it.end) }

        return Stat(
            avgStart = starts.map { it.hour + it.minute / 60.0 }.average(),
            avgEnd = ends.map { it.hour + it.minute / 60.0 }.average(),
        )
    }
}
