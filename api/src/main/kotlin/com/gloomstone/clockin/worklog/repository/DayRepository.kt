package com.gloomstone.clockin.worklog.repository

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.worklog.domain.Workday
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DayRepository : JpaRepository<Workday, Long> {
    fun findByDateAndUser(now: LocalDate?, user: User?): Workday?
    fun findAllByUserOrderByDate(user: User?): List<Workday>
    fun findAllByDateGreaterThanEqualAndUserOrderByDate(date: LocalDate?, user: User?): List<Workday>
    fun findAllByDateLessThanEqualAndUserOrderByDate(date: LocalDate?, user: User?): List<Workday>
    fun findAllByDateGreaterThanEqualAndDateLessThanEqualAndUserOrderByDate(
        min: LocalDate?,
        max: LocalDate?,
        user: User?
    ): List<Workday>

    fun deleteAllByUser(user: User)
}
