package com.gloomstone.clockin.worklog.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
class Snapshot(

    @ManyToOne
    var workday: Workday? = null,

    @Id
    var id: Long = 1
) {
    override fun toString(): String {
        return """
            Snapshot {
                id=$id
                day=${workday?.date}
            }
        """.trimIndent()
    }
}
