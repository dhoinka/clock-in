package com.gloomstone.clockin.worklog.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/** Records that a year's holiday response was fetched and persisted successfully. */
@Entity
@Table(name = "holiday_sync")
class HolidaySync(
    @Id
    @Column(name = "sync_year")
    val year: Int,

    @Column(name = "fetched_at", nullable = false)
    val fetchedAt: LocalDateTime,
)
