package com.gloomstone.clockin.worklog.repository

import com.gloomstone.clockin.worklog.domain.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface EventRepository : JpaRepository<Event, Long> {
    fun findAllByOrderByStart(): List<Event>

    @Query(
        """
        select e from Event e
        where e.start <= :to
          and e.end >= :from
        order by e.start
        """
    )
    fun findAllOverlappingRange(
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): List<Event>
}
