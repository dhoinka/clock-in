package com.gloomstone.clockin.worklog.domain

import com.gloomstone.clockin.iam.domain.User
import jakarta.persistence.*
import java.time.Duration
import java.time.LocalDate

@Entity
@Table(name = "workday")
data class Workday(
    val date: LocalDate,
    @ManyToOne
    val user: User,
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
                user="${user.username}"
                bookings=${entries.size}
                gross=$gross
                balance=$balance
                isWorkday=$isWorkday
            }
        """.trimIndent()
    }
}
