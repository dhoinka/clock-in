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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.*

@SpringBootTest
class WorklogServiceOvertimeTests {

    private val logger = LoggerFactory.getLogger(WorklogServiceOvertimeTests::class.java)

    @Autowired
    lateinit var worklogService: WorklogService

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
    fun test() {
        var clock = Clock.fixed(
            Instant.parse("2021-12-01T08:00:00Z"), ZoneId.systemDefault()
        )
        val username = faker.credentials().username()
        val user = findOrCreate(username)

        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        val test = worklogService.getEntries(LocalDate.now(clock), user.username)
        logger.info("{}", test[0])
        assertThat(test[0].balance).isEqualTo(Duration.ofMinutes(30))
        logger.info("{}", test[1])
        assertThat(test[1].balance).isEqualTo(Duration.ofHours(1))
        logger.info("{}", test[2])
        assertThat(test[2].balance).isEqualTo(Duration.ofHours(1).plusMinutes(30))
    }

    @Test
    fun testWithUpdates() {
        var clock = Clock.fixed(
            Instant.parse("2021-12-01T08:00:00Z"), ZoneId.systemDefault()
        )
        val localDate = LocalDate.now(clock)

        val username = faker.credentials().username()
        val user = findOrCreate(username)

        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        worklogService.update(getEntryUpdateRequest(localDate), user.username)
        worklogService.update(getEntryUpdateRequest(localDate.plusDays(2)), user.username)
        val test = worklogService.getEntries(LocalDate.now(clock), user.username)
        logger.info("{}", test[0])
        assertThat(test[0].balance).isEqualTo(Duration.ofMinutes(90))
        logger.info("{}", test[1])
        assertThat(test[1].balance).isEqualTo(Duration.ofHours(2))
        logger.info("{}", test[2])
        assertThat(test[2].balance).isEqualTo(Duration.ofHours(3).plusMinutes(30))
    }

    @Test
    fun testWithHolidays() {
        var clock = Clock.fixed(
            Instant.parse("2022-01-01T08:00:00Z"), ZoneId.systemDefault()
        )

        val username = faker.credentials().username()
        val user = findOrCreate(username)

        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        logger.info(clock.instant().toString())
        recordEntryNormalDay(clock, user)
        val test = worklogService.getEntries(LocalDate.now(clock), user.username)
        logger.info("{}", test[0])
        assertThat(test[0].balance).isEqualTo(Duration.ofHours(8).plusMinutes(30))
        logger.info("{}", test[1])
        assertThat(test[1].balance).isEqualTo(Duration.ofHours(17))
        logger.info("{}", test[2])
        assertThat(test[2].balance).isEqualTo(Duration.ofHours(17).plusMinutes(30))
    }

    @Test
    fun testWithCorrection() {
        var clock = Clock.fixed(
            Instant.parse("2021-12-01T09:00:00Z"), ZoneId.systemDefault()
        )
        val username = faker.credentials().username()
        val user = findOrCreate(username)

        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))
        recordEntryNormalDay(clock, user)
        clock = Clock.offset(clock, Duration.ofDays(1))

        val request = getEntryRequestWithCorrection(LocalDate.of(2021, 12, 1))
        worklogService.update(request, user.username)

        worklogService.getBalance(user.username)
        val test = worklogService.getEntries(LocalDate.now(clock), user.username)


        for (day in test) {
            logger.info("{}", day)
        }

        assertThat(test[0].gross).isEqualTo(Duration.ofHours(8).plusMinutes(30))
        assertThat(test[1].gross).isEqualTo(Duration.ofHours(8).plusMinutes(30))
        assertThat(test[2].gross).isEqualTo(Duration.ofHours(8).plusMinutes(30))

        assertThat(test[0].balance).isEqualTo(Duration.ofHours(5).plusMinutes(30))
        assertThat(test[1].balance).isEqualTo(Duration.ofHours(6))
        assertThat(test[2].balance).isEqualTo(Duration.ofHours(6).plusMinutes(30))
    }

    @Test
    fun testSnapshot() {
        val username = faker.credentials().username()
        val user = findOrCreate(username)

        for (i in 22..26) {
            val clock = Clock.fixed(
                Instant.parse("2021-11-${i}T09:00:00Z"), ZoneId.systemDefault()
            )
            worklogService.setClock(clock)

            val request = getEntryUpdateRequestHour(LocalDate.of(2021, 11, i))
            worklogService.update(request, user.username)
        }

        for (i in 29..29) {
            val clock = Clock.fixed(
                Instant.parse("2021-11-${i}T09:00:00Z"), ZoneId.systemDefault()
            )
            worklogService.setClock(clock)

            val request = getEntryUpdateRequestHour(LocalDate.of(2021, 11, i))
            worklogService.update(request, user.username)
        }

        val clock = Clock.fixed(
            Instant.parse("2021-12-01T09:00:00Z"), ZoneId.systemDefault()
        )
        worklogService.setClock(clock)

        val requset = getEntryUpdateRequestHour(LocalDate.of(2021, 12, 1))
        worklogService.update(requset, user.username)

        val test1 = worklogService.getEntries(LocalDate.of(2021, 11, 1), user.username)
        for (day in test1) {
            logger.info("{}", day)
        }

        val test2 = worklogService.getEntries(LocalDate.now(clock), user.username)
        for (day in test2) {
            logger.info("{}", day)
        }

        assertThat(test1[28].balance).isEqualTo(Duration.ofHours(6))
        assertThat(test1[29].balance).isEqualTo(Duration.ofHours(-2))

        assertThat(test2[0].balance).isEqualTo(Duration.ofHours(-1))

    }

    @Test
    fun testALot() {
        var clock = Clock.fixed(
            Instant.parse("2021-12-01T09:00:00Z"), ZoneId.systemDefault()
        )
        val username = faker.credentials().username()
        val user = findOrCreate(username)

        for (i in 0..30) {
            recordEntryNormalDay(clock, user)
            clock = Clock.offset(clock, Duration.ofDays(1))
        }
        val test = worklogService.getEntries(LocalDate.now(clock), user.username)
        for (day in test) {
            logger.info("{}", day)
        }
    }

    private fun recordEntryNormalDay(clock: Clock, user: User) {
        var anClock: Clock = clock
        worklogService.setClock(anClock)

        worklogService.recordEntry(user.username)
        anClock = Clock.offset(anClock, Duration.ofHours(9))
        worklogService.setClock(anClock)
        worklogService.recordEntry(user.username)
    }

    private fun getEntryUpdateRequestHour(localDate: LocalDate): UpdateWorkdayRequest {
        val l1 = LocalDateTime.of(localDate, LocalTime.of(8, 0))
        val l2 = LocalDateTime.of(localDate, LocalTime.of(17, 30))
        return UpdateWorkdayRequest(localDate, listOf(TimeEntryResponse("standard", l1, l2)))
    }

    private fun getEntryUpdateRequest(localDate: LocalDate): UpdateWorkdayRequest {
        val l1 = LocalDateTime.of(localDate, LocalTime.of(8, 0))
        val l2 = LocalDateTime.of(localDate, LocalTime.of(18, 0))
        return UpdateWorkdayRequest(localDate, listOf(TimeEntryResponse("standard", l1, l2)))
    }

    private fun getEntryRequestWithCorrection(localDate: LocalDate): UpdateWorkdayRequest {
        val l1 = LocalDateTime.of(localDate, LocalTime.of(9, 0))
        val l2 = LocalDateTime.of(localDate, LocalTime.of(18, 0))
        return UpdateWorkdayRequest(
            localDate,
            listOf(
                TimeEntryResponse("standard", l1, l2),
                TimeEntryResponse("correction", duration = "5h")
            )
        )
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
