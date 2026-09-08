package com.gloomstone.clockin.worklog.domain

import jakarta.persistence.*
import java.time.Duration
import java.time.LocalDate

@Entity
@Table(name = "workday")
data class Workday(
    val date: LocalDate,
    @OneToMany(mappedBy = "workday", fetch = FetchType.EAGER)
    @OrderBy("start NULLS LAST")
    var entries: MutableList<TimeEntry> = mutableListOf(),

    var gross: Duration? = null,

    var balance: Duration? = null,

    @Transient
    var isWorkday: Boolean = false,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
) {
    override fun toString(): String {
        return """
            Day {
                id=$id
                date=$date
                bookings=${entries.size}
                gross=$gross
                balance=$balance
                isWorkday=$isWorkday
            }
        """.trimIndent()
    }
}
