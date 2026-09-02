package com.gloomstone.clockin.worklog.domain

import com.gloomstone.clockin.iam.domain.User
import jakarta.persistence.*

@Entity
data class Snapshot(

    @ManyToOne
    var workday: Workday? = null,

    @ManyToOne
    var user: User? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
) {
    override fun toString(): String {
        return """
            Snapshot {
                id=$id
                day=${workday?.date}
                user="${user?.username}"
            }
        """.trimIndent()
    }
}
