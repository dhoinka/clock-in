package com.gloomstone.clockin.worklog.controller

import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.testutil.token
import com.gloomstone.clockin.worklog.domain.Setting
import com.gloomstone.clockin.worklog.mapper.SettingMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

@SpringBootTest
@AutoConfigureMockMvc
internal class SettingControllerTest {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: JsonMapper

    @Autowired
    lateinit var settingMapper: SettingMapper

    @Autowired
    lateinit var userService: UserService

    @Test
    @Throws(Exception::class)
    fun testGet() {
        val username = "settings-get-user"
        createUser(username)

        mvc.perform(get("/settings").with(token(username)))
            .andExpect(status().isOk)
    }

    @Test
    @Throws(Exception::class)
    fun testSome() {
        val username = "settings-update-user"
        val user = createUser(username)
        val request =
            settingMapper.toDto(Setting(Duration.ofHours(8).plusMinutes(30), Duration.ofMinutes(30), 31, user))
        mvc.perform(
            get("/settings")
                .with(token(username))
        )
            .andExpect(status().isOk)

        mvc.perform(
            put("/settings")
                .with(token(username))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)

        mvc.perform(get("/settings").with(token(username)))
            .andExpect(status().isOk)

    }

    private fun createUser(username: String) =
        userService.findByIdentity(username) ?: userService.create(
            CreateUserRequest(
                username = username,
                email = "$username@example.org",
                name = username,
                active = true,
                password = "test-password",
                passwordRepeat = "test-password",
            )
        )
}
