package com.gloomstone.clockin.worklog.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.Duration

@Entity
class Setting(
    var workingHours: Duration,
    var breakTime: Duration,
    var workingDays: Long = 0,
    @Id
    var id: Long = 1
)
