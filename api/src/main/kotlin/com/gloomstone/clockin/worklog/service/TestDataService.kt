package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.service.UserService
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
import java.time.LocalDateTime

@Service
@Profile("dev")
class TestDataService(
    private val timeEntryRepository: TimeEntryRepository,
    private val dayRepository: DayRepository,
    private val userService: UserService
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(TestDataService::class.java)

    override fun run(args: ApplicationArguments) {
        this.createTestData()
    }

    @Transactional
    fun createTestData() {
        if (userService.findByIdentity(ADMIN_USERNAME) != null) {
            return
        }
        val user = userService.create(
            CreateUserRequest(
                username = ADMIN_USERNAME,
                email = "$ADMIN_USERNAME@example.org",
                name = ADMIN_USERNAME,
                active = true,
                roles = listOf("admin"),
                password = ADMIN_PASSWORD,
                passwordRepeat = ADMIN_PASSWORD,
            )
        )

        var now = LocalDateTime.now().minusDays(30).withHour(8).withDayOfMonth(6)
        for (i in 0 until 30) {
            createFullDay(now, user)
            now = now.plusDays(1)
        }
    }

    fun createFullDay(now: LocalDateTime, user: User) {
        var workday = dayRepository.save(Workday(now.toLocalDate(), user))
        workday = dayRepository.save(workday)
        timeEntryRepository.save(TimeEntry(now, now.plusHours(9), workday, user))
        logger.info("Created normal day for {}, {}", now, now.dayOfWeek)
    }

    fun createHalfDay(now: LocalDateTime, user: User) {
        var workday = dayRepository.save(Workday(now.toLocalDate(), user))
        workday = dayRepository.save(workday)
        timeEntryRepository.save(TimeEntry(now, workday, user))
        logger.info("Created half day for {}, {}", now, now.dayOfWeek)
    }

    companion object {
        const val ADMIN_USERNAME = "admin"
        const val ADMIN_PASSWORD = "some-password"
    }
}
