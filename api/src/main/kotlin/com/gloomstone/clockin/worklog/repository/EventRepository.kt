package com.gloomstone.clockin.worklog.repository

import com.gloomstone.clockin.worklog.domain.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface EventRepository : JpaRepository<Event, Long> {
    fun findAllByOrderByStart(): List<Event>

    @Query("from Event e where ?1 >= e.start and ?2 <= e.end")
    fun findAllByStartGreaterThanEqualAndEndLessThanEqual(
        start: LocalDate,
        end: LocalDate
    ): List<Event>

}
