package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.worklog.domain.EntryType
import com.gloomstone.clockin.worklog.domain.TimeEntry
import com.gloomstone.clockin.worklog.dto.TimeEntryResponse
import com.gloomstone.clockin.worklog.dto.UpdateWorkdayRequest
import com.gloomstone.clockin.worklog.repository.DayRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import net.datafaker.Faker
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.*

@SpringBootTest
class WorklogSpecialCasesTests {

    @Autowired
    lateinit var worklogService: WorklogService

    @Autowired
    lateinit var timeEntryRepository: TimeEntryRepository

    @Autowired
    lateinit var dayRepository: DayRepository

    @Autowired
    lateinit var userService: UserService

    @MockitoBean
    lateinit var holidayService: HolidayService

    val faker = Faker()

    @BeforeEach
    fun beforeEach() {
        given(holidayService.getHolidays(LocalDate.now())).willReturn(emptyList())
    }

    @Test
    fun book() {
        var clock = Clock.fixed(
            Instant.parse("2020-01-01T08:00:00Z"), ZoneId.systemDefault()
        )
        val username = faker.credentials().username()
        val user = findOrCreate(username)

        worklogService.setClock(clock)
        for (i in 1..15) {
            val date1 = LocalDate.of(2020, 1, i)
            val request = UpdateWorkdayRequest(
                date1, mutableListOf(
                    TimeEntryResponse(
                        "standard",
                        LocalDateTime.of(date1, LocalTime.of(8, 0)),
                        LocalDateTime.of(date1, LocalTime.of(16, 30)),
                    )
                )
            )
            worklogService.update(request, user.username)
            clock = Clock.offset(clock, Duration.ofDays(1).minusHours(9))
        }

        val day = dayRepository.findByDateAndUser(LocalDate.of(2020, 1, 1), user) ?: fail()
        timeEntryRepository.save(
            TimeEntry("-8h", EntryType.CORRECTION, day, user)
        )


        val balance2 = worklogService.getBalance(user.username)
        println(balance2)
    }

    fun findOrCreate(username: String): User {
        val password = faker.credentials().password()
        return userService.findByIdentity(username) ?: userService.create(
            CreateUserRequest(
                username,
                faker.internet().emailAddress(),
                faker.name().fullName(),
                password = password,
                passwordRepeat = password
            )
        )
    }

}
