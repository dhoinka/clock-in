package com.gloomstone.clockin.iam.service

import com.gloomstone.clockin.iam.domain.Account
import com.gloomstone.clockin.iam.domain.Role
import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.PasswordRequestReset
import com.gloomstone.clockin.iam.dto.UserDto
import com.gloomstone.clockin.iam.mapper.UserMapper
import com.gloomstone.clockin.shared.exception.AuthenticationException
import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.shared.exception.InternalServerException
import com.gloomstone.clockin.shared.security.JwtUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceTests {
    private val userService: UserService = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private val userMapper: UserMapper = mock()
    private val jwtUtil = JwtUtil("test-secret")
    private val sessionService: SessionService = mock()

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(userService, passwordEncoder, userMapper, jwtUtil, sessionService)
    }

    @Test
    fun `authenticate normalizes identity and returns tokens for an active user`() {
        val user = user()
        val dto = userDto(user)
        whenever(userService.findByIdentity("alice")).thenReturn(user)
        whenever(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true)
        whenever(userMapper.toDto(user)).thenReturn(dto)
        whenever(sessionService.create(user.id)).thenReturn("refresh-token")

        val credentials = authService.authenticate("ALICE", "password123")

        assertThat(credentials.user).isEqualTo(dto)
        assertThat(credentials.accessToken).isNotBlank()
        assertThat(credentials.refreshToken).isNotBlank()
        verify(userService).update(user)
    }

    @Test
    fun `authenticate rejects invalid credentials`() {
        val user = user()
        whenever(userService.findByIdentity("alice")).thenReturn(user)
        whenever(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false)

        assertThatThrownBy {
            authService.authenticate("alice", "wrong-password")
        }.isInstanceOf(AuthenticationException::class.java)
            .hasMessage("Wrong username or password")

        verify(userService, never()).update(any())
    }

    @Test
    fun `authenticate rejects inactive user after verifying password`() {
        val inactiveUser = user(active = false)
        whenever(userService.findByIdentity("alice")).thenReturn(inactiveUser)
        whenever(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true)

        assertThatThrownBy {
            authService.authenticate("alice", "password123")
        }.isInstanceOf(AuthenticationException::class.java)

        verify(userService, never()).update(any())
    }

    @Test
    fun `authenticate checks a dummy password when the user does not exist`() {
        whenever(userService.findByIdentity("unknown")).thenReturn(null)
        whenever(passwordEncoder.matches(any(), any())).thenReturn(false)

        assertThatThrownBy {
            authService.authenticate("unknown", "password123")
        }.isInstanceOf(AuthenticationException::class.java)

        verify(passwordEncoder).matches("password123", DUMMY_PASSWORD_HASH)
        verify(userService, never()).update(any())
    }

    @Test
    fun `refresh accepts a valid refresh token for an active user`() {
        val user = user()
        val dto = userDto(user)
        whenever(userService.findByIdentity("alice-id")).thenReturn(user)
        whenever(userMapper.toDto(user)).thenReturn(dto)
        val oldToken = "opaque-refresh-token"
        whenever(sessionService.rotate(oldToken))
            .thenReturn(SessionService.Rotation(user.id, "new-refresh-token"))

        val credentials = authService.refresh(oldToken)

        assertThat(credentials.user).isEqualTo(dto)
        assertThat(credentials.accessToken).isNotBlank()
        assertThat(credentials.refreshToken).isNotBlank()
        verify(userService).update(user)
    }

    @Test
    fun `refresh rejects an inactive or missing user`() {
        val oldToken = "opaque-refresh-token"
        whenever(sessionService.rotate(oldToken))
            .thenReturn(SessionService.Rotation("alice-id", "new-refresh-token"))
        whenever(userService.findByIdentity("alice-id")).thenReturn(user(active = false))

        assertThatThrownBy {
            authService.refresh(oldToken)
        }.isInstanceOf(AuthenticationException::class.java)

        verify(sessionService).revoke("new-refresh-token")
        verify(userService, never()).update(any())
    }

    @Test
    fun `resetPassword encodes and saves a matching password`() {
        val user = user()
        whenever(userService.findByIdentity("alice")).thenReturn(user)
        whenever(passwordEncoder.encode("new-password")).thenReturn("new-encoded-password")

        authService.resetPassword(PasswordRequestReset("alice", "new-password", "new-password"))

        assertThat(user.account?.password).isEqualTo("new-encoded-password")
        verify(userService).update(user)
    }

    @Test
    fun `resetPassword rejects mismatched passwords without looking up a user`() {
        assertThatThrownBy {
            authService.resetPassword(PasswordRequestReset("alice", "new-password", "different"))
        }.isInstanceOf(Exception::class.java)
            .hasMessage("Passwords do not match")

        verify(userService, never()).findByIdentity(any())
        verify(userService, never()).update(any())
    }

    @Test
    fun `self reset rejects changing another users password`() {
        val user = user(username = "alice")
        whenever(userService.findByIdentity("alice-id")).thenReturn(user)

        assertThatThrownBy {
            authService.resetPassword(
                PasswordRequestReset("bob", "new-password", "new-password"),
                "alice-id",
            )
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Cannot change password for other user")

        verify(userService, never()).update(any())
    }

    @Test
    fun `self reset rejects a user without an account`() {
        whenever(userService.findByIdentity("alice-id")).thenReturn(user(account = null))

        assertThatThrownBy {
            authService.resetPassword(
                PasswordRequestReset("alice", "new-password", "new-password"),
                "alice-id",
            )
        }.isInstanceOf(InternalServerException::class.java)
            .hasMessage("User has no account")

        verify(userService, never()).update(any())
    }

    private fun user(
        username: String = "alice",
        account: Account? = Account(password = "encoded-password"),
        active: Boolean = true,
    ) = User(
        id = "alice-id",
        username = username,
        email = "$username@example.com",
        name = "Alice",
        account = account,
        roles = mutableListOf(Role("manager")),
        active = active,
    )

    private fun userDto(user: User) = UserDto(
        id = user.id,
        username = user.username,
        email = user.email,
        name = user.name,
        active = user.active,
        roles = user.roles.map { it.name },
    )

    private companion object {
        const val DUMMY_PASSWORD_HASH = "\$2a\$10\$fJBXxXlEEjYIi37lYvqZQ.TDoDDeKpK3r5qXtI4gJN5/8P04KXfbG"
    }
}
