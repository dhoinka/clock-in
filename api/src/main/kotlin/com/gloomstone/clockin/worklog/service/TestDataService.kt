package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.worklog.domain.TimeEntry
import com.gloomstone.clockin.worklog.domain.Workday
import com.gloomstone.clockin.worklog.repository.DayRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

@Service
@Profile("dev")
class TestDataService(
    private val timeEntryRepository: TimeEntryRepository,
    private val dayRepository: DayRepository,
    private val clock: Clock,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(TestDataService::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        createTestData()
    }

    fun createTestData() {
        if (dayRepository.count() > 0) return

        var now = LocalDateTime.now(clock).minusDays(30).withHour(8).withDayOfMonth(6)
        repeat(30) {
            createFullDay(now)
            now = now.plusDays(1)
        }
    }

    fun createFullDay(now: LocalDateTime) {
        val workday = dayRepository.save(Workday(now.toLocalDate()))
        timeEntryRepository.save(TimeEntry(now, now.plusHours(9), workday))
        logger.info("Created normal day for {}, {}", now, now.dayOfWeek)
    }

    fun createHalfDay(now: LocalDateTime) {
        val workday = dayRepository.save(Workday(now.toLocalDate()))
        timeEntryRepository.save(TimeEntry(now, workday))
        logger.info("Created half day for {}, {}", now, now.dayOfWeek)
    }
}
