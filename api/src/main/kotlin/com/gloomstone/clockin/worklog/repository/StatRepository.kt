package com.gloomstone.clockin.worklog.repository

import com.gloomstone.clockin.worklog.dto.StatMapping
import jakarta.persistence.EntityManager
import jakarta.persistence.Tuple
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Component
class StatRepository(private val em: EntityManager) {
    fun getStat(): List<StatMapping> {
        return em.createNativeQuery(
            """
            select u.username , date_trunc('month', d.date) as da, count(b.start_ts) as count
            from time_entry b join workday d on b.workday_id = d.id join users u on d.user_id = u.id
            group by u.username , da
        """.trimIndent(),
            Tuple::class.java
        ).resultList.map { i ->
            val tuple = i as Tuple


            if (tuple["da"] is Instant) {
                StatMapping(
                    tuple["username"] as String,
                    (tuple["da"] as Instant).atZone(ZoneId.systemDefault()).toLocalDate(),
                    tuple["count"] as Long
                )
            } else {
                StatMapping(
                    tuple["username"] as String,
                    LocalDate.parse((tuple["da"] as LocalDate).toString()),
                    tuple["count"] as Long
                )
            }
        }
    }

}