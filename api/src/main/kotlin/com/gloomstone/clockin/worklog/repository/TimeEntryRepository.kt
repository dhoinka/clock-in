package com.gloomstone.clockin.worklog.repository

import com.gloomstone.clockin.worklog.domain.EntryType
import com.gloomstone.clockin.worklog.domain.TimeEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TimeEntryRepository : JpaRepository<TimeEntry, Long> {

    fun findAllByType(type: EntryType): List<TimeEntry>
}
