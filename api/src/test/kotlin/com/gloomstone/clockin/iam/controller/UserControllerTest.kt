package com.gloomstone.clockin.iam.controller

import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.mapper.UserMapper
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.testutil.token
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var userMapper: UserMapper

    @Autowired
    lateinit var objectMapper: JsonMapper

    val faker = Faker()

    @BeforeEach
    fun beforeEach() {
        // Mock setup moved to individual tests to avoid timing issues with @MockitoBean
    }

    @Test
    fun `test GET me without token returns unauthorized`() {
        mvc.perform(get("/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `test GET me with invalid token returns unauthorized`() {
        mvc.perform(
            get("/me")
                .header("Authorization", "Bearer invalid-token")
        )
            .andExpect(status().isUnauthorized)
    }


    @Test
    fun `test GET me returns user details`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val password = faker.credentials().password()
        val user = this.userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        mvc.perform(get("/me").with(token(username)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(user.email))
    }

    @Test
    fun `test PUT me updates user details`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val password = faker.credentials().password()
        val user = this.userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        val updatedName = faker.name().fullName()

        val updatedUserDto = userMapper.toDto(user).copy(
            name = updatedName
        )

        mvc.perform(
            put("/me").with(token(username))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUserDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value(updatedUserDto.username))
            .andExpect(jsonPath("$.name").value(updatedName))
    }

    @Test
    fun `test GET user by id returns user by id`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val password = faker.credentials().password()
        val user = this.userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        mvc.perform(get("/users/${user.id}").with(token()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(user.id))
            .andExpect(jsonPath("$.email").value(user.email))
    }
}
