package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.config.AppConfig
import com.gloomstone.clockin.config.security.JwtResourceServerConfig
import com.gloomstone.clockin.config.security.WebSecurityConfig
import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.testutil.token
import com.gloomstone.clockin.worklog.domain.Setting
import com.gloomstone.clockin.worklog.dto.SettingResponse
import com.gloomstone.clockin.worklog.mapper.SettingMapper
import com.gloomstone.clockin.worklog.service.SettingService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

@WebMvcTest(SettingController::class)
@Import(WebSecurityConfig::class, JwtResourceServerConfig::class, AppConfig::class)
internal class SettingControllerTest {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: JsonMapper

    @MockitoBean
    lateinit var settingService: SettingService

    @MockitoBean
    lateinit var settingMapper: SettingMapper

    @MockitoBean
    lateinit var userService: UserService

    @Test
    fun `GET returns the authenticated user's settings`() {
        val setting = setting("alice")
        val response = SettingResponse("08:30", "00:30", 31)
        given(settingService.findByUser("alice")).willReturn(setting)
        given(settingMapper.toDto(setting)).willReturn(response)

        mvc.perform(get("/settings").with(token("alice")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.workingHours").value("08:30"))
    }

    @Test
    fun `PUT updates settings for the authenticated user`() {
        val user = user("alice")
        val request = SettingResponse("08:30", "00:30", 31)
        val setting = setting("alice")
        given(userService.findByIdentity("alice")).willReturn(user)
        given(settingService.update(request, user)).willReturn(setting)
        given(settingMapper.toDto(setting)).willReturn(request)

        mvc.perform(
            put("/settings")
                .with(token("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.workingDays").value(31))
    }

    @Test
    fun `PUT rejects an empty request body`() {
        mvc.perform(
            put("/settings")
                .with(token("alice"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    private fun setting(username: String) = Setting(
        workingHours = Duration.ofHours(8),
        breakTime = Duration.ofMinutes(30),
        workingDays = 31,
        user = user(username),
    )

    private fun user(username: String) = User(
        email = "$username@example.org",
        username = username,
        name = username,
        active = true,
        id = "user-$username",
    )
}
