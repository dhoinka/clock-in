package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.worklog.domain.Event
import com.gloomstone.clockin.worklog.dto.EventDto
import com.gloomstone.clockin.worklog.mapper.EventMapper
import com.gloomstone.clockin.worklog.service.EventService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(EventController::class)
class EventControllerTest {
    @Autowired
    lateinit var mvc: MockMvc
    @MockitoBean
    lateinit var service: EventService
    @MockitoBean
    lateinit var mapper: EventMapper

    @Test
    fun `filters events using the requested inclusive date range`() {
        val from = LocalDate.of(2026, 9, 1)
        val to = LocalDate.of(2026, 9, 30)
        val event = Event(id = 1, title = "Vacation", start = from, end = to)
        whenever(service.findAll(from, to)).thenReturn(listOf(event))
        whenever(mapper.toDto(event)).thenReturn(EventDto(id = 1, title = "Vacation", start = from, end = to))

        mvc.perform(get("/events?from=2026-09-01&to=2026-09-30"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(1))

        verify(service).findAll(from, to)
    }

    @Test
    fun `rejects an incomplete date filter`() {
        mvc.perform(get("/events?from=2026-09-01"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `creates an event from the frontend allDay JSON contract`() {
        val request = EventDto(
            title = "Vacation",
            type = "vacation",
            start = LocalDate.of(2026, 9, 14),
            end = LocalDate.of(2026, 9, 16),
            status = "new",
            allDay = true,
        )
        val event = Event(id = 7, title = "Vacation", start = request.start, end = request.end)
        whenever(service.create(request)).thenReturn(event)
        whenever(mapper.toDto(event)).thenReturn(request.copy(id = 7))

        mvc.perform(
            post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"title":"Vacation","type":"vacation","start":"2026-09-14","end":"2026-09-16","status":"new","allDay":true}"""
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(7))

        verify(service).create(request)
    }
}
