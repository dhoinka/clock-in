package com.gloomstone.clockin.iam.service

import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.dto.UpdateUserRequest
import com.gloomstone.clockin.iam.mapper.UserMapper
import com.gloomstone.clockin.iam.util.mockEncode
import com.gloomstone.clockin.iam.util.mockMatches
import net.datafaker.Faker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * Created by daniel on 15.06.2017.
 */
@SpringBootTest
class UserServiceTests {
    @Autowired
    lateinit var userService: UserService

    @MockitoBean
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var userMapper: UserMapper

    val faker = Faker()

    @BeforeEach
    fun before() {
        given(passwordEncoder.encode(anyString())).will { mockEncode(it) }
        given(passwordEncoder.matches(anyString(), anyString())).will { mockMatches(it) }
    }

    @Test
    fun `test Create`() {
        val email = faker.internet().emailAddress()
        val username = faker.credentials().username()
        val password = "test password"
        val dto = CreateUserRequest(
            username = username,
            email = email,
            active = true,
            password = password,
            passwordRepeat = password

        )

        var user = userService.create(dto)
        user = userService.findByIdentity(user.id) ?: fail("User not found")

        assertThat(user.id).isNotEmpty()
        assertThat(user.email).isEqualTo(email)
        assertThat(user.account).isNotNull
        assertThat(user.account?.password).isNotNull
        assertThat(user.account?.password).isNotEqualTo(password)
        assertThat(passwordEncoder.matches(password, user.account?.password)).isEqualTo(true)
    }

    @Test
    fun `test create`() {
        val users = userService.findAll()
        assertThat(users).isNotNull
    }

    @Test
    fun `test findAll with data`() {
        val email1 = faker.internet().emailAddress()
        val userName1 = faker.credentials().username()

        val email2 = faker.internet().emailAddress()
        val userName2 = faker.credentials().username()

        val email3 = faker.internet().emailAddress()
        val userName3 = faker.credentials().username()

        userService.create(
            CreateUserRequest(
                username = userName1,
                email = email1,
                password = "test",
                passwordRepeat = "test"

            )
        )
        userService.create(
            CreateUserRequest(
                username = userName2,
                email = email2,
                password = "test",
                passwordRepeat = "test"

            )
        )
        userService.create(
            CreateUserRequest(
                username = userName3,
                email = email3,
                password = "test",
                passwordRepeat = "test"

            )
        )

        val users = userService.findAll()
        assertThat(users).isNotNull
        assertThat(users.size).isGreaterThanOrEqualTo(3)
    }

    @Test
    fun `test findOneByEmail`() {
        val username = faker.credentials().username()
        val email = faker.internet().emailAddress()
        val password = faker.credentials().password()
        val dto = CreateUserRequest(
            username = username,
            email = email,
            active = true,
            password = password,
            passwordRepeat = password
        )
        val user = userService.create(dto)

        val foundUser = user.id.let { userService.findByIdentity(it) } ?: fail("User not found")
        assertThat(foundUser.id).isEqualTo(user.id)
        assertThat(foundUser.account).isNotNull
    }

    @Test
    fun `test findOne`() {
        val username = faker.credentials().username()
        val email = faker.internet().emailAddress()
        val password = faker.credentials().password()
        val dto = CreateUserRequest(
            username = username,
            email = email,
            active = true,
            password = password,
            passwordRepeat = password
        )
        val user = userService.create(dto)

        user.id.let { userService.findByIdentity(it) } ?: fail("User not found")
        username.let { userService.findByIdentity(it) } ?: fail("User not found")
        email.let { userService.findByIdentity(it) } ?: fail("User not found")
    }

    @Test
    fun `test update`() {
        val username = faker.credentials().username()
        val email = faker.internet().emailAddress()
        val password = "test password"
        val dto = CreateUserRequest(
            username = username,
            email = email,
            active = true,
            password = password,
            passwordRepeat = password
        )
        val user = userService.create(dto)

        val newEmail = faker.internet().emailAddress()
        user.email = newEmail
        var updatedUser = userService.update(
            user.id, UpdateUserRequest(
                email = newEmail
            )
        )

        updatedUser = userService.findByIdentity(updatedUser.id) ?: fail("User not found")
        assertThat(updatedUser.id).isEqualTo(user.id)
        assertThat(updatedUser.email).isEqualTo(newEmail)

    }

    @Test
    fun `test changePassword`() {
        val password = faker.credentials().password()
        val newPassword = faker.credentials().password()

        val username = faker.credentials().username()
        val email = faker.internet().emailAddress()

        val dto = CreateUserRequest(
            username = username,
            email = email,
            active = true,
            password = password,
            passwordRepeat = password
        )

        var user = userService.create(dto)
        val oldPassword = user.account!!.password
        assertThat(user.id).isNotEmpty()
        assertThat(user.account).isNotNull

        user = userService.changePassword(user, newPassword)
        user = userService.findByIdentity(user.id) ?: fail("User not found")

        assertThat(user.id).isNotEmpty()
        assertThat(user.account).isNotNull
        assertThat(user.account!!.password).isNotEqualTo(oldPassword)
        assertThat(passwordEncoder.matches(newPassword, user.account!!.password)).isEqualTo(true)
    }

    @Test
    fun `test remove`() {
        val username = faker.credentials().username()
        val email = faker.internet().emailAddress()
        val password = faker.credentials().password()

        val dto = CreateUserRequest(
            username = username,
            email = email,
            active = true,
            password = password,
            passwordRepeat = password
        )

        val user = userService.create(dto)
        assertThat(user.id).isNotEmpty()
        user.id.let { userService.delete(it) }
        user.id.let { assertThat(userService.findByIdentity(it)).isNull() }
    }
}