package com.gloomstone.clockin.worklog.domain

import com.gloomstone.clockin.iam.domain.User
import jakarta.persistence.*
import java.time.LocalDateTime


@Entity
class TimeEntry {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: EntryType = EntryType.STANDARD

    @ManyToOne
    var user: User? = null

    @ManyToOne
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

    constructor(start: LocalDateTime, workday: Workday, user: User) {
        this.start = start
        this.workday = workday
        this.user = user
    }

    constructor(start: LocalDateTime, end: LocalDateTime, workday: Workday, user: User) {
        this.start = start
        this.end = end
        this.user = user
        this.workday = workday
    }

    constructor(duration: String, type: EntryType, workday: Workday, user: User) {
        this.duration = duration
        this.user = user
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
                user="${user?.username}",
            }
        """.trimIndent()
    }
}
