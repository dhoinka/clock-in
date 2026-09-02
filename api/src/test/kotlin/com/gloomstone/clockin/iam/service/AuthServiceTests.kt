package com.gloomstone.clockin.iam.service

import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.dto.PasswordRequestReset
import com.gloomstone.clockin.iam.util.mockEncode
import com.gloomstone.clockin.iam.util.mockMatches
import net.datafaker.Faker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * Created by daniel on 15.06.2017.
 */
@SpringBootTest
class AuthServiceTests {

    @Autowired
    lateinit var authService: AuthService

    @Autowired
    lateinit var userService: UserService

    @MockitoBean
    lateinit var passwordEncoder: PasswordEncoder

    @Value("\${app.jwt-secret}")
    lateinit var key: String

    val faker = Faker()


    @BeforeEach
    fun beforeEach() {
        given(passwordEncoder.encode(anyString())).will { mockEncode(it) }
        given(passwordEncoder.matches(anyString(), anyString())).will { mockMatches(it) }
    }

    @Test
    fun testLogin() {
        val username = faker.credentials().username()
        val email = faker.internet().emailAddress()
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

        val response = authService.authenticate(
            username = email,
            password = password,
        )
        assertThat(response).isNotNull
        assertThat(response.user).isNotNull
        assertThat(response.accessToken).isNotNull
    }

    @Test
    fun testLoginWithCreatedUser() {
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

        val response = authService.authenticate(
            username = email,
            password = password,
        )
        assertThat(response).isNotNull
        assertThat(response.user).isNotNull
        assertThat(response.user.id).isEqualTo(user.id)
        assertThat(response.accessToken).isNotNull
    }

    @Test
    fun `test login fail`() {
        val username = "none valid"
        val password = "none valid"
        try {
            authService.authenticate(
                username = username,
                password = password,
            )
        } catch (e: Exception) {
            assertThat(e).hasMessage("Wrong username or password")
        }
    }

    @Test
    fun `test reset password`() {
        val username = faker.credentials().username()
        val email = faker.internet().emailAddress()
        val password = faker.credentials().password()
        var user = userService.create(
            CreateUserRequest(
                username = username,
                email = email,
                active = true,
                password = password,
                passwordRepeat = password
            )
        )
        val oldPassword = user.account!!.password

        authService.resetPassword(PasswordRequestReset(email, "newPassword", "newPassword"))

        user = userService.findByIdentity(email) ?: fail("User not found")
        val newPassword = user.account!!.password
        assertThat(oldPassword).isNotEqualTo(newPassword)
    }
}
