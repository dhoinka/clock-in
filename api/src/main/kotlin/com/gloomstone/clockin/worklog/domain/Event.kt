package com.gloomstone.clockin.worklog.domain

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

    @Enumerated(EnumType.STRING)
    var status: EventStatus? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,


    )
