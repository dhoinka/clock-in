package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.config.AppConfig
import com.gloomstone.clockin.config.security.JwtResourceServerConfig
import com.gloomstone.clockin.config.security.WebSecurityConfig
import com.gloomstone.clockin.shared.testutil.token
import com.gloomstone.clockin.worklog.domain.Holiday
import com.gloomstone.clockin.worklog.service.HolidayService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(HolidayController::class)
@Import(WebSecurityConfig::class, JwtResourceServerConfig::class, AppConfig::class)
class HolidayControllerTest {
    @Autowired
    lateinit var mvc: MockMvc

    @MockitoBean
    lateinit var holidayService: HolidayService

    @Test
    fun `GET serializes holidays for the requested year`() {
        given(holidayService.getHolidays(any())).willReturn(
            listOf(Holiday(LocalDate.of(2026, 1, 1), "New Year"))
        )

        mvc.perform(get("/holidays/2026").with(token("alice")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].date").value("2026-01-01"))
            .andExpect(jsonPath("$[0].name").value("New Year"))
    }
}
