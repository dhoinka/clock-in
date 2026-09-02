package com.gloomstone.clockin.worklog.domain

import com.gloomstone.clockin.iam.domain.User
import jakarta.persistence.*
import java.time.Duration

@Entity
data class Setting(
    var workingHours: Duration,
    var breakTime: Duration,
    var workingDays: Long = 0,
    @ManyToOne
    var user: User,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)
