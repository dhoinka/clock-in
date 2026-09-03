package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.testutil.token
import com.gloomstone.clockin.worklog.domain.EntryType
import com.gloomstone.clockin.worklog.domain.TimeEntry
import com.gloomstone.clockin.worklog.domain.Workday
import com.gloomstone.clockin.worklog.dto.TimeEntryResponse
import com.gloomstone.clockin.worklog.dto.UpdateWorkdayRequest
import com.gloomstone.clockin.worklog.repository.DayRepository
import com.gloomstone.clockin.worklog.repository.TimeEntryRepository
import com.gloomstone.clockin.worklog.service.HolidayService
import com.gloomstone.clockin.worklog.service.WorklogService
import net.datafaker.Faker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@SpringBootTest
@AutoConfigureMockMvc
class WorklogControllerTest {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var timeEntryRepository: TimeEntryRepository

    @Autowired
    lateinit var worklogService: WorklogService

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var dayRepository: DayRepository

    @Autowired
    lateinit var mapper: JsonMapper

    @MockitoBean
    lateinit var holidayService: HolidayService

    val faker = Faker()


    @BeforeEach
    fun beforeEach() {
        given(holidayService.getHolidays(LocalDate.now())).willReturn(emptyList())
    }

    @Test
    fun `test GET`() {
        val username = faker.credentials().username()
        findOrCreate(username)

        mvc.perform(get("/status").with(token(username)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.checkedIn").value(false))
    }

    @Test
    fun `test POST`() {
        val username = faker.credentials().username()
        findOrCreate(username)

        mvc.perform(post("/status").with(token(username)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.checkedIn").value(true))
    }

    @Test
    fun `test GET days`() {
        val username = faker.credentials().username()
        val user = findOrCreate(username)

        var now = LocalDateTime.now().withDayOfMonth(1)
        var workday = dayRepository.save(Workday(now.toLocalDate(), user))
        var start = now.withHour(8)
        var end = now.withHour(17)
        timeEntryRepository.save(TimeEntry(start, workday, user))
        timeEntryRepository.save(TimeEntry(end, workday, user))
        now = now.plusDays(2)
        start = now.withHour(8)
        end = now.withHour(17)
        workday = dayRepository.save(Workday(now.toLocalDate(), user))
        timeEntryRepository.save(TimeEntry(start, workday, user))
        timeEntryRepository.save(TimeEntry(end, workday, user))

        mvc.perform(get("/workdays").with(token(username)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(now.toLocalDate().lengthOfMonth()))
    }

    @Test
    fun `test GET entries with query`() {
        val username = faker.credentials().username()
        findOrCreate(username)

        mvc.perform(get("/workdays/2020-01").with(token(username)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].date").value("2020-01-01"))

        mvc.perform(get("/workdays/2020-02").with(token(username)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].date").value("2020-02-01"))
    }

    @Test
    fun `test PUT`() {

        val date = LocalDate.now()
        val req = UpdateWorkdayRequest(
            date, mutableListOf(
                TimeEntryResponse(
                    "standard",
                    LocalDateTime.of(date, LocalTime.of(8, 0)),
                    LocalDateTime.of(date, LocalTime.of(12, 0)),
                ),
                TimeEntryResponse(
                    "standard",
                    LocalDateTime.of(date, LocalTime.of(13, 0)),
                    LocalDateTime.of(date, LocalTime.of(17, 0)),
                )
            )
        )

        val username = faker.credentials().username()
        findOrCreate(username)

        mvc.perform(
            put("/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req))
                .with(token(username))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `test PUT for unknown user returns bad request`() {
        val date = LocalDate.now()
        val req = UpdateWorkdayRequest(
            date, mutableListOf(
                TimeEntryResponse(
                    "standard",
                    LocalDateTime.of(date, LocalTime.of(8, 0)),
                    LocalDateTime.of(date, LocalTime.of(13, 0)),

                    ),
                TimeEntryResponse(
                    "standard",
                    LocalDateTime.of(date, LocalTime.of(12, 0)),
                    LocalDateTime.of(date, LocalTime.of(17, 0)),

                    )
            )
        )
        val username = faker.credentials().username()
        mvc.perform(
            put("/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req))
                .with(token(username))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `test GET stats`() {
        val date = LocalDate.now()
        val req = UpdateWorkdayRequest(
            date, mutableListOf(
                TimeEntryResponse(
                    "standard",
                    LocalDateTime.of(date, LocalTime.of(8, 0)),
                    LocalDateTime.of(date, LocalTime.of(17, 0)),
                )
            )
        )

        val username = faker.credentials().username()
        val user = findOrCreate(username)
        this.worklogService.update(req, user.username)

        mvc.perform(
            get("/worklog/stats")
                .contentType(MediaType.APPLICATION_JSON)
                .with(token(username))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value(username))
            .andExpect(jsonPath("$.avgStart").value(8))
            .andExpect(jsonPath("$.avgEnd").value(17))
    }

    @Test
    fun `test DELETE`() {
        val date = LocalDate.now()
        val req = UpdateWorkdayRequest(
            date, mutableListOf(
                TimeEntryResponse(
                    "standard",
                    LocalDateTime.of(date, LocalTime.of(8, 0)),
                    LocalDateTime.of(date, LocalTime.of(17, 0)),
                )
            )
        )
        val username = faker.credentials().username()
        val user = findOrCreate(username)

        this.worklogService.update(req, user.username)
        val allEntries = this.timeEntryRepository.findAllByTypeAndUser(EntryType.STANDARD, user)
        assertThat(allEntries).hasSize(1)

        mvc.perform(
            delete("/entries/${allEntries[0].id}")
                .contentType(MediaType.APPLICATION_JSON)
                .with(token(username))
        )
            .andExpect(status().isOk)

        val allBookingsAgain = this.timeEntryRepository.findAllByTypeAndUser(EntryType.STANDARD, user)
        assertThat(allBookingsAgain).hasSize(0)
    }

    @Test
    fun `test GET corrections`() {
        val date = LocalDate.now()
        val req = UpdateWorkdayRequest(
            date, mutableListOf(
                TimeEntryResponse(
                    "standard",
                    LocalDateTime.of(date, LocalTime.of(8, 0)),
                    LocalDateTime.of(date, LocalTime.of(17, 0)),
                ),
                TimeEntryResponse(
                    type = "correction",
                    duration = "8h 30m"
                )
            )
        )

        val username = faker.credentials().username()
        val user = findOrCreate(username)
        this.worklogService.update(req, user.username)

        mvc.perform(
            get("/entries?type=correction")
                .contentType(MediaType.APPLICATION_JSON)
                .with(token(username))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].duration").value("8h 30m"))
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
