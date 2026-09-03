package com.gloomstone.clockin.iam.controller.mgmt

import com.gloomstone.clockin.config.AppConfig
import com.gloomstone.clockin.config.security.JwtResourceServerConfig
import com.gloomstone.clockin.config.security.WebSecurityConfig
import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.dto.PasswordRequestReset
import com.gloomstone.clockin.iam.dto.UpdateUserRequest
import com.gloomstone.clockin.iam.dto.UserDto
import com.gloomstone.clockin.iam.mapper.UserMapper
import com.gloomstone.clockin.iam.service.AuthService
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.exception.NotFoundException
import com.gloomstone.clockin.shared.testutil.token
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

@WebMvcTest(UserMgmtController::class)
@Import(WebSecurityConfig::class, JwtResourceServerConfig::class, AppConfig::class)
class UserMgmtControllerTests {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: JsonMapper

    @MockitoBean
    lateinit var userService: UserService

    @MockitoBean
    lateinit var authService: AuthService

    @MockitoBean
    lateinit var userMapper: UserMapper

    @Test
    fun `POST creates a user`() {
        val request = CreateUserRequest(
            username = "alice",
            email = "alice@example.org",
            password = "test-password",
            passwordRepeat = "test-password",
        )
        val user = user("alice")
        given(userService.create(request)).willReturn(user)
        given(userMapper.toDto(user)).willReturn(dto(user))

        mvc.perform(
            post("/mgmt/users")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("user-alice"))
            .andExpect(jsonPath("$.email").value("alice@example.org"))
    }

    @Test
    fun `reset password rejects a missing request body`() {
        mvc.perform(
            post("/mgmt/users/user-alice/reset-password")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET returns mapped users`() {
        val user = user("alice")
        given(userService.findAll()).willReturn(listOf(user))
        given(userMapper.toDto(user)).willReturn(dto(user))

        mvc.perform(get("/mgmt/users").with(token()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].username").value("alice"))
    }

    @Test
    fun `GET one returns not found for an unknown user`() {
        given(userService.findByIdentity("missing")).willReturn(null)

        mvc.perform(get("/mgmt/users/missing").with(token()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT updates a user`() {
        val request = UpdateUserRequest(email = "new@example.org", active = true)
        val user = user("alice").copy(email = "new@example.org")
        given(userService.update("user-alice", request)).willReturn(user)
        given(userMapper.toDto(user)).willReturn(dto(user))

        mvc.perform(
            put("/mgmt/users/user-alice")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("new@example.org"))
    }

    @Test
    fun `DELETE delegates to the service`() {
        doNothing().`when`(userService).delete("user-alice")

        mvc.perform(delete("/mgmt/users/user-alice").with(token()))
            .andExpect(status().isOk)
    }

    @Test
    fun `reset password delegates valid data and maps missing users to not found`() {
        val request = PasswordRequestReset("alice", "new-password", "new-password")
        doNothing().`when`(authService).resetPassword(request, "user-alice")

        mvc.perform(
            post("/mgmt/users/user-alice/reset-password")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)

        given(authService.resetPassword(request, "missing")).willThrow(NotFoundException())
        mvc.perform(
            post("/mgmt/users/missing/reset-password")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `management endpoints reject authenticated users without admin authority`() {
        mvc.perform(get("/mgmt/users").with(token("alice")))
            .andExpect(status().isForbidden)
    }

    private fun user(username: String) = User(
        email = "$username@example.org",
        username = username,
        name = username,
        active = true,
        id = "user-$username",
    )

    private fun dto(user: User) = UserDto(
        id = user.id,
        username = user.username,
        email = user.email,
        name = user.name,
        active = user.active,
    )
}
