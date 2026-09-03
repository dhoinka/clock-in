package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.config.AppConfig
import com.gloomstone.clockin.config.security.JwtResourceServerConfig
import com.gloomstone.clockin.config.security.WebSecurityConfig
import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.shared.testutil.token
import com.gloomstone.clockin.worklog.domain.Event
import com.gloomstone.clockin.worklog.domain.EventType
import com.gloomstone.clockin.worklog.dto.EventDto
import com.gloomstone.clockin.worklog.mapper.EventMapper
import com.gloomstone.clockin.worklog.service.EventService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.doNothing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

@WebMvcTest(EventController::class)
@Import(WebSecurityConfig::class, JwtResourceServerConfig::class, AppConfig::class)
internal class EventControllerTest {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: JsonMapper

    @MockitoBean
    lateinit var eventService: EventService

    @MockitoBean
    lateinit var eventMapper: EventMapper

    @Test
    fun `GET returns events belonging to the authenticated user`() {
        val event = event("alice", 1)
        val dto = dto(event)
        given(eventService.findAllByUsername("alice")).willReturn(listOf(event))
        given(eventMapper.toDto(event)).willReturn(dto)

        mvc.perform(get("/events").with(token("alice")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].title").value("Vacation"))
            .andExpect(jsonPath("$[0].username").value("alice"))
    }

    @Test
    fun `POST forwards an event using the authenticated username`() {
        val request = EventDto(
            title = "Vacation",
            type = "vacation",
            start = LocalDate.of(2026, 9, 1),
            end = LocalDate.of(2026, 9, 2),
        )
        val event = event("alice", 2)
        given(eventService.create(request, "alice")).willReturn(event)
        given(eventMapper.toDto(event)).willReturn(dto(event))

        mvc.perform(
            post("/events")
                .with(token("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.username").value("alice"))
    }

    @Test
    fun `PUT assigns the path id before updating`() {
        val request = EventDto(
            id = 3,
            title = "Updated vacation",
            type = "vacation",
            start = LocalDate.of(2026, 9, 1),
            end = LocalDate.of(2026, 9, 2),
        )
        val event = event("alice", 3).copy(title = "Updated vacation")
        given(eventService.update(request, "alice")).willReturn(event)
        given(eventMapper.toDto(event)).willReturn(dto(event))

        mvc.perform(
            put("/events/3")
                .with(token("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request.copy(id = null)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.title").value("Updated vacation"))
    }

    @Test
    fun `DELETE forwards the event id and authenticated username`() {
        doNothing().`when`(eventService).delete(4, "alice")

        mvc.perform(delete("/events/4").with(token("alice")))
            .andExpect(status().isOk)
    }

    @Test
    fun `event endpoints require authentication`() {
        mvc.perform(get("/events"))
            .andExpect(status().isUnauthorized)
    }

    private fun event(username: String, id: Long) = Event(
        title = "Vacation",
        type = EventType.VACATION,
        start = LocalDate.of(2026, 9, 1),
        end = LocalDate.of(2026, 9, 2),
        user = User("$username@example.org", username, username, active = true),
        id = id,
    )

    private fun dto(event: Event) = EventDto(
        id = event.id,
        title = event.title,
        type = event.type?.value,
        start = event.start,
        end = event.end,
        username = event.user?.username,
    )
}
