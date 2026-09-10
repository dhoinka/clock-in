package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.worklog.domain.EntryType
import com.gloomstone.clockin.worklog.domain.TimeEntry
import com.gloomstone.clockin.worklog.domain.Workday
import com.gloomstone.clockin.worklog.util.toDuration
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

internal data class WorkdayTotals(
    val gross: Duration,
    val balance: Duration,
)

/** Deterministic work-time calculations without repository or service access. */
internal class WorklogCalculator {
    fun calculate(
        workday: Workday,
        previousBalance: Duration?,
        workingHours: Duration,
        breakTime: Duration,
        now: LocalDateTime,
        isWorkday: Boolean,
        snapshotBalance: Duration?,
    ): WorkdayTotals {
        val gross = calculateGross(workday.entries, now, breakTime)
        val checkedIn = isCheckedIn(workday.entries)

        var balance = when {
            workday.entries.isNotEmpty() && !checkedIn ->
                completedDayBalance(previousBalance, gross, workingHours, isWorkday)

            isWorkday && workday.date != now.toLocalDate() ->
                previousBalance?.minus(workingHours) ?: workingHours.negated()

            else -> previousBalance ?: workday.balance ?: Duration.ZERO
        }

        workday.entries
            .asSequence()
            .filter { it.type == EntryType.CORRECTION }
            .forEach { correction ->
                val text = correction.duration
                    ?: throw BadRequestException("Correction entries require a duration")
                val duration = text.toDuration()
                    ?: throw BadRequestException("Invalid correction duration: $text")
                balance = balance.plus(duration)
            }

        return WorkdayTotals(gross, snapshotBalance ?: balance)
    }

    fun isCheckedIn(entries: List<TimeEntry>): Boolean =
        entries.any { it.type == EntryType.STANDARD && it.start != null && it.end == null }

    private fun calculateGross(
        entries: List<TimeEntry>,
        now: LocalDateTime,
        breakTime: Duration,
    ): Duration {
        val startOfDay = now.with(LocalTime.MIDNIGHT)
        val durations = entries
            .asSequence()
            .filter { it.type == EntryType.STANDARD }
            .map { entry ->
                val start = entry.start
                val end = entry.end

                when {
                    start == null -> Duration.ZERO
                    end != null -> Duration.between(start, end)
                    start.isAfter(startOfDay) -> Duration.between(start, now)
                    else -> Duration.ZERO
                }
            }
            .toList()

        val gross = durations.fold(Duration.ZERO, Duration::plus)
        return if (durations.firstOrNull()?.let { it > SIX_HOURS } == true) {
            gross.minus(breakTime)
        } else {
            gross
        }
    }

    private fun completedDayBalance(
        previousBalance: Duration?,
        gross: Duration,
        workingHours: Duration,
        isWorkday: Boolean,
    ): Duration {
        if (previousBalance == null) {
            return if (isWorkday) gross.minus(workingHours) else gross
        }

        return when {
            !isWorkday -> previousBalance.plus(gross)
            gross.isNegative -> previousBalance.plus(gross)
            else -> previousBalance.plus(gross).minus(workingHours)
        }
    }

    companion object {
        private val SIX_HOURS = Duration.ofHours(6)
    }
}
