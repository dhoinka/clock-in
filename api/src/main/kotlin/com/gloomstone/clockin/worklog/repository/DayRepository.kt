package com.gloomstone.clockin.worklog.repository

import com.gloomstone.clockin.worklog.domain.Workday
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DayRepository : JpaRepository<Workday, Long> {
    fun findByDate(date: LocalDate): Workday?
    fun findAllByOrderByDate(): List<Workday>
    fun findAllByDateGreaterThanEqualOrderByDate(date: LocalDate): List<Workday>
    fun findAllByDateLessThanEqualOrderByDate(date: LocalDate): List<Workday>
    fun findAllByDateGreaterThanEqualAndDateLessThanEqualOrderByDate(min: LocalDate, max: LocalDate): List<Workday>
}
