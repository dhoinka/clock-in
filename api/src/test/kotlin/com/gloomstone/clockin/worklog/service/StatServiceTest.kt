package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.repository.UserRepository
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.worklog.dto.TimeEntryResponse
import com.gloomstone.clockin.worklog.dto.UpdateWorkdayRequest
import com.gloomstone.clockin.worklog.repository.StatRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.*

@SpringBootTest
class StatServiceTest {
    @Autowired
    lateinit var worklogService: WorklogService

    @Autowired
    lateinit var statService: StatService

    @Autowired
    lateinit var userService: UserService

    @MockitoBean
    lateinit var holidayService: HolidayService

    @BeforeEach
    fun beforeEach() {
        given(holidayService.getHolidays(LocalDate.now())).willReturn(emptyList())
    }

    @Test
    fun testGetStats() {
        val clock = Clock.fixed(
            Instant.parse("2020-01-01T08:00:00Z"), ZoneId.systemDefault()
        )
        worklogService.setClock(clock)
        val user = findOrCreate("stats-user")
        for (i in 1 until 10) {
            val date = LocalDate.of(2020, 1, i)
            val request = UpdateWorkdayRequest(
                date, mutableListOf(
                    TimeEntryResponse(
                        "standard",
                        LocalDateTime.of(date, LocalTime.of(if (i % 2 == 0) 7 else 8, 0)),
                        LocalDateTime.of(date, LocalTime.of(17, 0)),
                    )
                )
            )

            worklogService.update(request, user.username)
        }
        val stats = statService.getStats(user.username)

        println(stats)
    }

    @Test
    fun testSomethingWeird() {
        val timeEntryRepository = mock(TimeEntryRepository::class.java)
        val userRepository = mock(UserRepository::class.java)
        val statRepository = mock(StatRepository::class.java)
        // daniel, 8.36638418079096045198, 17.3209039548022598870

        val map = mapOf(
            Pair("username", "daniel"),
            Pair("start_ts", "8.36638418079096045198"),
            Pair("end_ts", "17.3209039548022598870")
        )

        given(timeEntryRepository.getStats("1")).willReturn(map)
        timeEntryRepository.getStats("1")

        val user = User("admin", "admin", "admin", id = "1")
        val mockedUserService = mock(UserService::class.java)
        given(mockedUserService.findByIdentity(user.username)).willReturn(user)
        val statService1 = StatService(timeEntryRepository, userRepository, statRepository, mockedUserService)

        val stats = statService1.getStats(user.username)

        assertThat(stats.username).isEqualTo("daniel")
        assertThat(stats.avgStart).isEqualTo(8.366384180790961)
        assertThat(stats.avgEnd).isEqualTo(17.32090395480226)
    }

    private fun findOrCreate(username: String): User =
        userService.findByIdentity(username) ?: userService.create(
            CreateUserRequest(
                username = username,
                email = "$username@example.org",
                name = username,
                active = true,
                password = "test-password",
                passwordRepeat = "test-password",
            )
        )

}
