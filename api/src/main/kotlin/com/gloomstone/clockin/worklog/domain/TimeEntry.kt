package com.gloomstone.clockin.worklog.domain

import jakarta.persistence.*
import java.time.LocalDateTime


@Entity
class TimeEntry {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: EntryType = EntryType.STANDARD

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    var workday: Workday? = null

    // TimeEntry
    @Column(name = "start_ts")
    var start: LocalDateTime? = null

    @Column(name = "end_ts")
    var end: LocalDateTime? = null

    // Correction
    var duration: String? = null

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    constructor(start: LocalDateTime, workday: Workday) {
        this.start = start
        this.workday = workday
    }

    constructor(start: LocalDateTime, end: LocalDateTime, workday: Workday) {
        this.start = start
        this.end = end
        this.workday = workday
    }

    constructor(duration: String, type: EntryType, workday: Workday) {
        this.duration = duration
        this.workday = workday
        this.type = type
    }

    override fun toString(): String {
        return """
            Booking {
                id=$id
                type=$type
                start=$start
                end=$end
                duration=$duration
            }
        """.trimIndent()
    }
}
