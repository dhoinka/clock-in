package com.gloomstone.clockin.worklog.domain

import com.gloomstone.clockin.iam.domain.User
import jakarta.persistence.*
import java.time.LocalDate

@Entity
data class Event(
    var title: String? = null,

    @Enumerated(EnumType.STRING)
    var type: EventType? = null,

    @Column(name = "start_date")
    var start: LocalDate? = null,

    @Column(name = "end_date")
    var end: LocalDate? = null,
    @Column(name = "all_day")
    var isAllDay: Boolean = true,

    @ManyToOne
    var user: User? = null,

    @Enumerated(EnumType.STRING)
    var status: EventStatus? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,


    )
