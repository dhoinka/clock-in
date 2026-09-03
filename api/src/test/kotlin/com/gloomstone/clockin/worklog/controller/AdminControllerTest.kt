package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.config.AppConfig
import com.gloomstone.clockin.config.security.JwtResourceServerConfig
import com.gloomstone.clockin.config.security.WebSecurityConfig
import com.gloomstone.clockin.shared.testutil.token
import com.gloomstone.clockin.worklog.dto.StatMapping
import com.gloomstone.clockin.worklog.service.StatService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(AdminController::class)
@Import(WebSecurityConfig::class, JwtResourceServerConfig::class, AppConfig::class)
class AdminControllerTest {
    @Autowired
    lateinit var mvc: MockMvc

    @MockitoBean
    lateinit var statService: StatService

    @Test
    fun `admin statistics are available to administrators`() {
        given(statService.getAdminStats()).willReturn(
            mapOf("alice" to mutableListOf(StatMapping("alice", LocalDate.of(2026, 9, 1), 3)))
        )

        mvc.perform(get("/admin/stats").with(token()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.alice[0].count").value(3))
    }

    @Test
    fun `admin statistics reject authenticated users without admin authority`() {
        mvc.perform(get("/admin/stats").with(token("alice")))
            .andExpect(status().isForbidden)
    }

}
