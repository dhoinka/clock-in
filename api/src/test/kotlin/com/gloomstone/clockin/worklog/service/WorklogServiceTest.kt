package com.gloomstone.clockin.worklog.service

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.worklog.dto.TimeEntryResponse
import com.gloomstone.clockin.worklog.dto.UpdateWorkdayRequest
import net.datafaker.Faker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.*

@SpringBootTest
class WorklogServiceTest {
    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var worklogService: WorklogService

    @MockitoBean
    lateinit var holidayService: HolidayService

    lateinit var clock: Clock

    val faker = Faker()

    lateinit var user: User

    @BeforeEach
    fun beforeEach() {
        given(holidayService.getHolidays(LocalDate.now())).willReturn(emptyList())
        clock = Clock.fixed(
            Instant.parse("2020-01-01T08:00:00Z"), ZoneId.systemDefault()
        )
        worklogService.setClock(clock)

        user = findOrCreate(faker.credentials().username())
    }

    @Test
    fun recordEntryOnlyIn() {
        val statusResponse = worklogService.recordEntry(user.username)
        assertThat(statusResponse.isCheckedIn).isEqualTo(true)
        assertThat(statusResponse.gross).isEqualTo(Duration.ZERO)
        clock = Clock.offset(clock, Duration.ofHours(3))
        worklogService.setClock(clock)
        val balance = worklogService.getBalance(user.username)
        assertThat(balance.gross).isEqualTo(Duration.ofHours(3))
    }

    @Test
    fun recordEntryInAndOut() {
        var statusResponse = worklogService.recordEntry(user.username)
        assertThat(statusResponse.isCheckedIn).isEqualTo(true)
        assertThat(statusResponse.gross).isEqualTo(Duration.ZERO)

        clock = Clock.offset(clock, Duration.ofHours(2))
        worklogService.setClock(clock)
        statusResponse = worklogService.recordEntry(user.username)
        assertThat(statusResponse.isCheckedIn).isEqualTo(false)
        assertThat(statusResponse.gross).isEqualTo(Duration.ofHours(2))
    }

    @Test
    fun recordEntryInAndOutAndInAndOut() {
        var statusResponse = worklogService.recordEntry(user.username)
        assertThat(statusResponse.isCheckedIn).isEqualTo(true)
        assertThat(statusResponse.gross).isEqualTo(Duration.ZERO)

        clock = Clock.offset(clock, Duration.ofHours(2))
        worklogService.setClock(clock)
        statusResponse = worklogService.recordEntry(user.username)
        assertThat(statusResponse.isCheckedIn).isEqualTo(false)
        assertThat(statusResponse.gross).isEqualTo(Duration.ofHours(2))

        clock = Clock.offset(clock, Duration.ofHours(2))
        worklogService.setClock(clock)
        statusResponse = worklogService.recordEntry(user.username)
        assertThat(statusResponse.isCheckedIn).isEqualTo(true)
        assertThat(statusResponse.gross).isEqualTo(Duration.ofHours(2))
    }

    @Test
    fun recordEntryInAndOutAndIn() {
        var statusResponse = worklogService.recordEntry(user.username)
        assertThat(statusResponse.isCheckedIn).isEqualTo(true)
        assertThat(statusResponse.gross).isEqualTo(Duration.ZERO)

        clock = Clock.offset(clock, Duration.ofHours(2))
        worklogService.setClock(clock)
        statusResponse = worklogService.recordEntry(user.username)
        assertThat(statusResponse.isCheckedIn).isEqualTo(false)
        assertThat(statusResponse.gross).isEqualTo(Duration.ofHours(2))

        clock = Clock.offset(clock, Duration.ofHours(2))
        worklogService.setClock(clock)
        statusResponse = worklogService.recordEntry(user.username)
        assertThat(statusResponse.isCheckedIn).isEqualTo(true)
        assertThat(statusResponse.gross).isEqualTo(Duration.ofHours(2))

        clock = Clock.offset(clock, Duration.ofHours(2))
        worklogService.setClock(clock)
        statusResponse = worklogService.recordEntry(user.username)
        assertThat(statusResponse.isCheckedIn).isEqualTo(false)
        assertThat(statusResponse.gross).isEqualTo(Duration.ofHours(4))
    }

    @Test
    fun testNormalWorkDay() {
        var statusResponse = worklogService.recordEntry(user.username)
        assertThat(statusResponse.isCheckedIn).isEqualTo(true)
        assertThat(statusResponse.gross).isEqualTo(Duration.ZERO)

        clock = Clock.offset(clock, Duration.ofHours(8).plusMinutes(30))
        worklogService.setClock(clock)
        statusResponse = worklogService.recordEntry(user.username)
        assertThat(statusResponse.gross).isEqualTo(Duration.ofHours(8))
        assertThat(statusResponse.isCheckedIn).isFalse
    }

    @Test
    fun testGetBalance() {
        worklogService.setClock(clock)
        worklogService.recordEntry(user.username)
        clock = Clock.offset(clock, Duration.ofHours(8).plusMinutes(30))
        worklogService.setClock(clock)
        worklogService.recordEntry(user.username)
        var balance = worklogService.getEntries(LocalDate.now(clock), user.username)
        assertThat(balance[0].entries.size).isEqualTo(1)
        assertThat(balance[0].gross).isEqualTo(Duration.ofHours(8))
        clock = Clock.fixed(
            Instant.parse("2020-01-02T08:00:00Z"), ZoneId.systemDefault()
        )
        worklogService.setClock(clock)
        worklogService.recordEntry(user.username)
        clock = Clock.offset(clock, Duration.ofHours(8).plusMinutes(30))
        worklogService.setClock(clock)
        worklogService.recordEntry(user.username)
        balance = worklogService.getEntries(LocalDate.now(clock).minusDays(1), user.username)
        assertThat(balance[1].entries.size).isEqualTo(1)
        assertThat(balance[1].gross).isEqualTo(Duration.ofHours(8))
        assertThat(balance.size).isEqualTo(31)
    }

    @Test
    fun testForgettingEntry() {
        worklogService.setClock(clock)
        worklogService.recordEntry(user.username)
        clock = Clock.offset(clock, Duration.ofHours(5))
        worklogService.setClock(clock)
        worklogService.recordEntry(user.username)
        clock = Clock.offset(clock, Duration.ofDays(1))
        worklogService.setClock(clock)
        worklogService.recordEntry(user.username)
        val balance = worklogService.getEntries(LocalDate.now(clock), user.username)
        assertThat(balance[0].gross).isEqualTo(Duration.ofHours(5))
        assertThat(balance[1].gross).isEqualTo(Duration.ZERO)
    }

    @Test
    fun testAWeek() {
        worklogService.setClock(clock)
        worklogService.recordEntry(user.username)
        clock = Clock.offset(clock, Duration.ofHours(9))
        worklogService.setClock(clock)
        worklogService.recordEntry(user.username)
        for (i in 0..3) {
            clock = Clock.offset(clock, Duration.ofDays(1).minusHours(9))
            worklogService.setClock(clock)
            worklogService.recordEntry(user.username)
            clock = Clock.offset(clock, Duration.ofHours(9))
            worklogService.setClock(clock)
            worklogService.recordEntry(user.username)
        }
        val summary = worklogService.getEntries(LocalDate.now(clock), user.username)
        println(summary)
    }

    @Test
    fun testUpdate() {
        val date = LocalDate.of(2020, 1, 1)
        val request = UpdateWorkdayRequest(
            date, listOf(
                TimeEntryResponse(
                    "standard",
                    LocalDateTime.of(date, LocalTime.of(8, 0)),
                    LocalDateTime.of(date, LocalTime.of(17, 0)),

                    )
            )
        )
        worklogService.update(request, user.username)
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
