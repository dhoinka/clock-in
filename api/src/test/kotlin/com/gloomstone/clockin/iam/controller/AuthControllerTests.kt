package com.gloomstone.clockin.iam.controller

import com.gloomstone.clockin.iam.dto.*
import com.gloomstone.clockin.iam.mapper.UserMapper
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.iam.util.mockEncode
import com.gloomstone.clockin.iam.util.mockMatches
import net.datafaker.Faker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

/**
 * Created by daniel on 19.06.2017.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var objectMapper: JsonMapper

    @Autowired
    lateinit var userMapper: UserMapper

    @Autowired
    lateinit var userService: UserService


    @MockitoBean
    lateinit var passwordEncoder: PasswordEncoder


    val faker = Faker()

    @BeforeEach
    fun beforeEach() {
        given(passwordEncoder.encode(anyString())).will { mockEncode(it) }
        given(passwordEncoder.matches(anyString(), anyString())).will { mockMatches(it) }

    }

    @Test
    fun `test POST login successful`() {
        val email = faker.internet().emailAddress()
        val password = faker.credentials().password()
        val user = createUser(email, password)

        val request = LoginRequest(
            username = user.email,
            password = password
        )
        mvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.user.email").value(user.email))
            .andExpect(jsonPath("$.user.username").value(user.username))
            .andExpect(jsonPath("$.user.name").value(user.name))

    }

    @Test
    fun `test POST login with email`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val password = faker.credentials().password()

        userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        val request = LoginRequest(
            username = email,
            password = password,
        )
        mvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.user.email").value(email))
    }

    @Test
    fun `test POST login fails unauthorized`() {
        val email = faker.internet().emailAddress()
        val password = faker.credentials().password()
        val request = LoginRequest(
            username = email,
            password = password,
        )
        mvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `test POST refresh token returns new tokens`() {
        val email = faker.internet().emailAddress()
        val password = faker.credentials().password()
        val user = createUser(email, password)

        val request = LoginRequest(
            username = user.email,
            password = password,
        )

        val result = mvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.user.email").value(user.email))
            .andReturn()

        val loginResponse = objectMapper.readValue(result.response.contentAsString, Credentials::class.java)
        assertThat(loginResponse.refreshToken).isNotNull
        assertThat(loginResponse.accessToken).isNotNull

        val refreshRequest = RefreshRequest(loginResponse.refreshToken)

        mvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.user.email").value(user.email))
    }

    @Test
    fun `test POST signup creates user`() {
        val username = faker.credentials().username()
        val email = faker.internet().emailAddress()
        val password = faker.credentials().password()

        val request = SignUpDto(
            username = username,
            email = email,
            password = password,
            passwordConfirm = password
        )

        mvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.user.email").value(email))
            .andExpect(jsonPath("$.user.username").value(username))
    }

    @Test
    fun `test POST signup username collision returns conflict`() {
        val existingUsername = faker.credentials().username()
        val existingEmail = faker.internet().emailAddress()
        val password = faker.credentials().password()

        // create an existing user with that username
        userService.create(
            CreateUserRequest(
                username = existingUsername,
                email = existingEmail,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        val request = SignUpDto(
            username = existingUsername,
            email = faker.internet().emailAddress(),
            password = password,
            passwordConfirm = password
        )

        mvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `test POST signup email collision returns conflict`() {
        val existingUsername = faker.credentials().username()
        val existingEmail = faker.internet().emailAddress()
        val password = faker.credentials().password()

        // create an existing user with that email
        userService.create(
            CreateUserRequest(
                username = existingUsername,
                email = existingEmail,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        // Attempt to signup without username (username will default to email) to trigger email collision
        val request = mapOf(
            "username" to existingUsername,
            "email" to existingEmail,
            "password" to password,
            "passwordConfirm" to password
        )

        mvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }


    private fun createUser(email: String, password: String): UserDto {
        val user = userService.create(
            CreateUserRequest(
                username = email,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        return this.userMapper.toDto(user)
    }


}
