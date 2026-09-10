package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.worklog.domain.Stat
import com.gloomstone.clockin.worklog.domain.Status
import com.gloomstone.clockin.worklog.dto.StatusResponse
import com.gloomstone.clockin.worklog.mapper.WorklogMapper
import com.gloomstone.clockin.worklog.service.StatService
import com.gloomstone.clockin.worklog.service.WorklogService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration
import java.time.Clock

@WebMvcTest(WorklogController::class)
class WorklogControllerTest {
    @Autowired lateinit var mvc: MockMvc
    @MockitoBean lateinit var service: WorklogService
    @MockitoBean lateinit var mapper: WorklogMapper
    @MockitoBean lateinit var stats: StatService
    @MockitoBean lateinit var clock: Clock

    @Test
    fun `status endpoint is available without authentication`() {
        val statusValue = Status(false, Duration.ZERO, Duration.ZERO)
        whenever(service.getBalance()).thenReturn(statusValue)
        whenever(mapper.toDto(statusValue)).thenReturn(StatusResponse(false, "0m", "0m"))

        mvc.perform(get("/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.checkedIn").value(false))
    }

    @Test
    fun `personal stats are globally available without authentication`() {
        whenever(stats.getStats()).thenReturn(Stat(8.5, 17.25))

        mvc.perform(get("/worklog/stats"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.avgStart").value(8.5))
            .andExpect(jsonPath("$.avgEnd").value(17.25))
    }
}
