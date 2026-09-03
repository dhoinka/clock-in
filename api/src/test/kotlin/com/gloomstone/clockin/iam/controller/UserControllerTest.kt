package com.gloomstone.clockin.iam.controller

import com.gloomstone.clockin.config.AppConfig
import com.gloomstone.clockin.config.security.JwtResourceServerConfig
import com.gloomstone.clockin.config.security.WebSecurityConfig
import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.UpdateSelfRequest
import com.gloomstone.clockin.iam.dto.UserDto
import com.gloomstone.clockin.iam.mapper.UserMapper
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.testutil.token
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

@WebMvcTest(UserController::class)
@Import(WebSecurityConfig::class, JwtResourceServerConfig::class, AppConfig::class)
class UserControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: JsonMapper

    @MockitoBean
    lateinit var userService: UserService

    @MockitoBean
    lateinit var userMapper: UserMapper

    @Test
    fun `GET me without token returns unauthorized`() {
        mvc.perform(get("/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET me with invalid token returns unauthorized`() {
        mvc.perform(get("/me").header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET me returns the authenticated user's details`() {
        val user = user("alice")
        val dto = dto(user)
        given(userService.findByIdentity("alice")).willReturn(user)
        given(userMapper.toDto(user)).willReturn(dto)

        mvc.perform(get("/me").with(token("alice")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("user-alice"))
            .andExpect(jsonPath("$.email").value("alice@example.org"))
    }

    @Test
    fun `PUT me validates and updates only the authenticated user`() {
        val original = user("alice")
        val updated = original.copy(name = "Alice Updated")
        val request = UpdateSelfRequest(name = "Alice Updated")
        given(userService.findByIdentity("alice")).willReturn(original)
        given(userService.updateSelf("user-alice", request)).willReturn(updated)
        given(userMapper.toDto(updated)).willReturn(dto(updated))

        mvc.perform(
            put("/me")
                .with(token("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.name").value("Alice Updated"))
    }

    @Test
    fun `PUT me returns a bad request for malformed JSON`() {
        mvc.perform(
            put("/me")
                .with(token("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET user by id is restricted to administrators`() {
        val user = user("alice")
        given(userService.findByIdentity("user-alice")).willReturn(user)
        given(userMapper.toDto(user)).willReturn(dto(user))

        mvc.perform(get("/users/user-alice").with(token()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("user-alice"))

    }

    private fun user(username: String) = User(
        email = "$username@example.org",
        username = username,
        name = username.replaceFirstChar(Char::uppercase),
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
