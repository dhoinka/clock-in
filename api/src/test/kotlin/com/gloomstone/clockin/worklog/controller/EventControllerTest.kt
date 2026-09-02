package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.testutil.token
import com.gloomstone.clockin.worklog.domain.Event
import com.gloomstone.clockin.worklog.domain.EventType
import com.gloomstone.clockin.worklog.mapper.EventMapper
import com.gloomstone.clockin.worklog.service.EventService
import net.datafaker.Faker
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
internal class EventControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var mapper: JsonMapper

    @Autowired
    private lateinit var eventService: EventService

    @Autowired
    private lateinit var eventMapper: EventMapper

    @Autowired
    private lateinit var userService: UserService

    val faker = Faker()

    @Test
    fun `test GET`() {
        val username = faker.credentials().username()
        findOrCreate(username)

        mvc.perform(get("/events").with(token(username)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `test POST`() {
        val username = faker.credentials().username()
        val user = findOrCreate(username)
        val event = eventMapper.toDto(
            Event(
                faker.lordOfTheRings().location(),
                EventType.VACATION,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                true,
                user
            )
        )
        mvc.perform(
            post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(event))
                .with(token(username))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isMap)
            .andExpect(jsonPath("$.title").value(event.title))
            .andExpect(jsonPath("$.type").value("vacation"))
            .andExpect(jsonPath("$.username").value(username))
    }

    @Test
    fun `test PUT`() {
        val username = faker.credentials().username()
        val user = findOrCreate(username)
        val eventDto = eventMapper.toDto(
            Event("some eventDto", EventType.NONE, LocalDate.now(), LocalDate.now().plusDays(1), true, user)
        )
        val event = eventService.create(eventDto, user.username)
        val eventUpdate = eventMapper.toDto(
            Event("some eventDto NEW", EventType.NONE, LocalDate.now(), LocalDate.now().plusDays(1), true, user)
        )
        mvc.perform(
            put("/events/" + event.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(eventUpdate))
                .with(token(username))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isMap)
            .andExpect(jsonPath("$.title").value("some eventDto NEW"))
            .andExpect(jsonPath("$.username").value(username))
    }

    @Test
    fun `test GET list`() {
        val username = faker.credentials().username()
        val user = findOrCreate(username)
        var now = LocalDateTime.now()
        for (i in 0..9) {
            val eventDto = eventMapper.toDto(
                Event("some eventDto", EventType.NONE, LocalDate.now(), LocalDate.now().plusDays(1), true, user)
            )
            eventService.create(eventDto, user.username)
            now = now.plusDays(1)
        }
        mvc.perform(
            get("/events")
                .with(token(username))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(10))
            .andExpect(jsonPath("$[0].title").value("some eventDto"))
            .andExpect(jsonPath("$[0].username").value(username))
    }

    @Test
    fun `test DELETE`() {
        val username = faker.credentials().username()
        val user = findOrCreate(username)
        val eventDto = eventMapper.toDto(
            Event("some eventDto", EventType.NONE, LocalDate.now(), LocalDate.now().plusDays(1), true, user)
        )
        val event = eventService.create(eventDto, user.username)
        mvc.perform(
            delete("/events/" + event.id)
                .with(token(username))
        )
            .andExpect(status().is2xxSuccessful)
    }

    @Test
    fun `test DELETE 4xx`() {
        val username = faker.credentials().username()
        val user = findOrCreate(username)
        val eventDto = eventMapper.toDto(
            Event("some eventDto", EventType.NONE, LocalDate.now(), LocalDate.now().plusDays(1), true, user)
        )
        val event = eventService.create(eventDto, user.username)
        mvc.perform(
            delete("/events/" + event.id)
                .with(token())
        )
            .andExpect(status().is4xxClientError)
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
