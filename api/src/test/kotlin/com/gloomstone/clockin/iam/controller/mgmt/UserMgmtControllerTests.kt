package com.gloomstone.clockin.iam.controller.mgmt

import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.dto.PasswordRequestReset
import com.gloomstone.clockin.iam.dto.UpdateUserRequest
import com.gloomstone.clockin.iam.mapper.UserMapper
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.iam.util.mockEncode
import com.gloomstone.clockin.iam.util.mockMatches
import com.gloomstone.clockin.shared.testutil.token
import net.datafaker.Faker
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

/**
 * Created by daniel on 20.06.2017.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserMgmtControllerTests {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var mapper: JsonMapper

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var userMapper: UserMapper

    @MockitoBean
    lateinit var passwordEncoder: PasswordEncoder

    val faker = Faker()

    @BeforeEach
    fun before() {
        BDDMockito.given(
            passwordEncoder.encode(
                ArgumentMatchers.anyString()
            )
        ).will { mockEncode(it) }
        BDDMockito.given(
            passwordEncoder.matches(
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString()
            )
        )
            .will { mockMatches(it) }

    }

    @Test
    fun `test POST`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val pw = faker.credentials().password()
        val createUserRequest = CreateUserRequest(
            username = username,
            email = email,
            active = true,
            password = pw,
            passwordRepeat = pw
        )
        mvc.perform(
            MockMvcRequestBuilders.post("/mgmt/users")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(createUserRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.username").value(username))
    }

    @Test
    fun `test GET`() {
        mvc.perform(MockMvcRequestBuilders.get("/mgmt/users").with(token()))
            .andExpect(status().isOk)
    }

    @Test
    fun `test GET one`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val password = faker.credentials().password()
        val user = userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        mvc.perform(MockMvcRequestBuilders.get("/mgmt/users/" + user.id).with(token()))
            .andExpect(status().isOk)
    }

    @Test
    fun `test GET with ID`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val password = faker.credentials().password()
        val user = userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        mvc.perform(MockMvcRequestBuilders.get("/mgmt/users/" + user.id).with(token()))
            .andExpect(status().isOk)
    }

    @Test
    fun `test GET one failed`() {
        mvc.perform(
            MockMvcRequestBuilders.get("/mgmt/users/99999999")
                .with(token())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `test PUT`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val password = faker.credentials().password()

        val user = userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        val newEmail = faker.internet().emailAddress()
        val newUser = user.copy(email = newEmail)

        val content = mapper.writeValueAsString(userMapper.toDto(newUser))

        mvc.perform(
            MockMvcRequestBuilders.put("/mgmt/users/" + user.id)
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(newEmail))
            .andExpect(jsonPath("$.id").value(user.id))
    }

    @Test
    fun `test PUT to set user active`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val password = faker.credentials().password()
        val user = userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = false,
                password = password,
                passwordRepeat = password
            )
        )

        Assertions.assertThat(user.active).isFalse

        user.apply { user.active = true }

        mvc.perform(
            MockMvcRequestBuilders.put("/mgmt/users/" + user.id)
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(userMapper.toDto(user)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value(user.username))
            .andExpect(jsonPath("$.active").value(true))
    }

    @Test
    fun `test PUT failed`() {
        mvc.perform(
            MockMvcRequestBuilders.put("/mgmt/users/999999").with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `test PUT partial`() {
        val username = faker.credentials().username()
        val email = faker.internet().emailAddress()
        val password = faker.credentials().password()
        val user = userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )
        val req = UpdateUserRequest(
            username = faker.credentials().username(),
            email = faker.internet().emailAddress(),
        )

        mvc.perform(
            MockMvcRequestBuilders.put("/mgmt/users/" + user.id).with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(req.email))
    }

    @Test
    fun `test DELETE`() {
        val username = faker.credentials().username()
        val email = faker.internet().emailAddress()
        val password = faker.credentials().password()

        val user = userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        mvc.perform(
            delete("/mgmt/users/" + user.id)
                .with(token())
        ).andExpect(status().isOk)

        val findOne = userService.findByIdentity(user.id)
        Assertions.assertThat(findOne).isNull()
    }

    @Test
    fun `test DELETE fail`() {
        mvc.perform(delete("/mgmt/users/5995654").with(token()))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `test POST reset-password`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val password = faker.credentials().password()
        val user = userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        val newPassword = faker.credentials().password()
        val passwordResetRequest = PasswordRequestReset(
            username = username,
            password = newPassword,
            passwordRepeat = newPassword
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/mgmt/users/${user.id}/reset-password")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(passwordResetRequest))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `test POST reset-password with null request`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val password = faker.credentials().password()
        val user = userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )


        mvc.perform(
            MockMvcRequestBuilders.post("/mgmt/users/${user.id}/reset-password")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `test POST reset-password with non-existent user`() {
        val newPassword = faker.credentials().password()
        val passwordResetRequest = PasswordRequestReset(
            username = faker.credentials().username(),
            password = newPassword,
            passwordRepeat = newPassword
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/mgmt/users/non-existent-id/reset-password")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(passwordResetRequest))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `test POST reset-password with mismatched passwords`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val password = faker.credentials().password()
        val user = userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )

        val newPassword = faker.credentials().password()
        val differentPassword = faker.credentials().password()
        val passwordResetRequest = PasswordRequestReset(
            username = username,
            password = newPassword,
            passwordRepeat = differentPassword
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/mgmt/users/${user.id}/reset-password")
                .with(token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(passwordResetRequest))
        )
            .andExpect(status().isBadRequest)
    }

}