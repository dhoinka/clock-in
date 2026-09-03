package com.gloomstone.clockin.iam.service

import com.gloomstone.clockin.iam.domain.Account
import com.gloomstone.clockin.iam.domain.Role
import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.dto.UpdateSelfRequest
import com.gloomstone.clockin.iam.dto.UpdateUserRequest
import com.gloomstone.clockin.iam.mapper.UserMapper
import com.gloomstone.clockin.iam.repository.AccountRepository
import com.gloomstone.clockin.iam.repository.RoleRepository
import com.gloomstone.clockin.iam.repository.UserRepository
import com.gloomstone.clockin.shared.exception.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTests {
    private val userRepository: UserRepository = mock()
    private val accountRepository: AccountRepository = mock()
    private val roleRepository: RoleRepository = mock()
    private val passwordEncoder: PasswordEncoder = mock()
    private val userMapper: UserMapper = mock()

    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = UserService(
            userRepository,
            accountRepository,
            roleRepository,
            passwordEncoder,
            userMapper,
        )
        whenever(accountRepository.save(any<Account>())).thenAnswer { it.arguments[0] as Account }
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] as User }
    }

    @Test
    fun `create normalizes identity, encodes password, and persists requested roles`() {
        val manager = Role("manager")
        whenever(roleRepository.findByName("manager")).thenReturn(manager)
        whenever(passwordEncoder.encode("password123")).thenReturn("encoded-password")

        val created = userService.create(
            createRequest(username = "Alice", email = "ALICE@EXAMPLE.COM", roles = listOf("manager"))
        )

        assertThat(created.username).isEqualTo("alice")
        assertThat(created.email).isEqualTo("alice@example.com")
        assertThat(created.active).isTrue()
        assertThat(created.account?.password).isEqualTo("encoded-password")
        assertThat(created.roles).containsExactly(manager)
        verify(accountRepository).save(created.account!!)
        verify(userRepository).save(created)
    }

    @Test
    fun `create rejects mismatched or empty passwords without persisting`() {
        assertThatThrownBy {
            userService.create(createRequest(password = "password123", passwordRepeat = "different"))
        }.isInstanceOf(PasswordNotEqualException::class.java)

        assertThatThrownBy {
            userService.create(createRequest(password = "", passwordRepeat = ""))
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Password is required")

        verify(accountRepository, never()).save(any<Account>())
        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `create rejects an existing normalized username`() {
        whenever(userRepository.findOneByUsername("alice")).thenReturn(user(username = "alice"))

        assertThatThrownBy {
            userService.create(createRequest(username = "Alice"))
        }.isInstanceOf(UsernameAlreadyExistsException::class.java)

        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `create rejects an unknown requested role`() {
        whenever(passwordEncoder.encode("password123")).thenReturn("encoded-password")
        whenever(roleRepository.findByName("unknown")).thenReturn(null)

        assertThatThrownBy {
            userService.create(createRequest(roles = listOf("unknown")))
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Role not found")

        verify(accountRepository, never()).save(any<Account>())
    }

    @Test
    fun `findByIdentity checks username then email then id`() {
        val found = user(id = "user-id")
        whenever(userRepository.findOneByUsername("user-id")).thenReturn(null)
        whenever(userRepository.findOneByEmail("user-id")).thenReturn(null)
        whenever(userRepository.findById("user-id")).thenReturn(found)

        assertThat(userService.findByIdentity("user-id")).isSameAs(found)

        verify(userRepository).findOneByUsername("user-id")
        verify(userRepository).findOneByEmail("user-id")
        verify(userRepository).findById("user-id")
    }

    @Test
    fun `findByIdentity stops after a username match`() {
        val found = user(username = "alice")
        whenever(userRepository.findOneByUsername("alice")).thenReturn(found)

        assertThat(userService.findByIdentity("alice")).isSameAs(found)

        verify(userRepository, never()).findOneByEmail(any())
        verify(userRepository, never()).findById(any<String>())
    }

    @Test
    fun `changePassword verifies the old password before saving encoded replacement`() {
        val user = user(account = Account(password = "old-encoded"))
        whenever(passwordEncoder.matches("old-password", "old-encoded")).thenReturn(true)
        whenever(passwordEncoder.encode("new-password")).thenReturn("new-encoded")

        val updated = userService.changePassword(user, "old-password", "new-password")

        assertThat(updated.account?.password).isEqualTo("new-encoded")
        verify(userRepository).save(user)
    }

    @Test
    fun `changePassword rejects an invalid old password`() {
        val user = user(account = Account(password = "old-encoded"))
        whenever(passwordEncoder.matches("wrong-password", "old-encoded")).thenReturn(false)

        assertThatThrownBy {
            userService.changePassword(user, "wrong-password", "new-password")
        }.isInstanceOf(AuthenticationException::class.java)

        verify(userRepository, never()).save(any<User>())
        verify(passwordEncoder, never()).encode(any())
    }

    @Test
    fun `update resolves requested roles before saving mapped user`() {
        val existing = user(id = "user-id", roles = mutableListOf(Role("user")))
        val manager = Role("manager")
        val request = UpdateUserRequest(name = "Alice Doe", roles = listOf("manager"))
        whenever(userRepository.findOneByUsername("user-id")).thenReturn(null)
        whenever(userRepository.findOneByEmail("user-id")).thenReturn(null)
        whenever(userRepository.findById("user-id")).thenReturn(existing)
        whenever(roleRepository.findByName("manager")).thenReturn(manager)
        doAnswer { invocation ->
            val target = invocation.arguments[1] as User
            target.name = "Alice Doe"
        }.whenever(userMapper).update(request, existing)

        val updated = userService.update("user-id", request)

        assertThat(updated.name).isEqualTo("Alice Doe")
        assertThat(updated.roles).containsExactly(manager)
        verify(userMapper).update(request, existing)
        verify(userRepository).save(existing)
    }

    @Test
    fun `updateSelf delegates only allowed fields to mapper and saves user`() {
        val existing = user(id = "user-id", roles = mutableListOf(Role("manager")), active = true)
        val request = UpdateSelfRequest(name = "New name")
        whenever(userRepository.findOneByUsername("user-id")).thenReturn(null)
        whenever(userRepository.findOneByEmail("user-id")).thenReturn(null)
        whenever(userRepository.findById("user-id")).thenReturn(existing)

        userService.updateSelf("user-id", request)

        verify(userMapper).updateSelf(request, existing)
        verify(userRepository).save(existing)
        assertThat(existing.roles).extracting<String> { it.name }.containsExactly("manager")
        assertThat(existing.active).isTrue()
    }

    @Test
    fun `delete ignores the protected admin account but deletes other users`() {
        val admin = user(id = "admin-id", username = "admin")
        val alice = user(id = "alice-id", username = "alice")
        whenever(userRepository.findOneByUsername("admin-id")).thenReturn(null)
        whenever(userRepository.findOneByEmail("admin-id")).thenReturn(null)
        whenever(userRepository.findById("admin-id")).thenReturn(admin)
        whenever(userRepository.findOneByUsername("alice-id")).thenReturn(null)
        whenever(userRepository.findOneByEmail("alice-id")).thenReturn(null)
        whenever(userRepository.findById("alice-id")).thenReturn(alice)

        userService.delete("admin-id")
        userService.delete("alice-id")

        verify(userRepository, never()).delete(admin)
        verify(userRepository).delete(alice)
    }

    @Test
    fun `delete rejects an unknown identity`() {
        assertThatThrownBy { userService.delete("missing") }
            .isInstanceOf(NotFoundException::class.java)
    }

    private fun createRequest(
        username: String = "alice",
        email: String = "alice@example.com",
        password: String = "password123",
        passwordRepeat: String = password,
        roles: List<String>? = null,
    ) = CreateUserRequest(
        username = username,
        email = email,
        active = true,
        password = password,
        passwordRepeat = passwordRepeat,
        roles = roles,
    )

    private fun user(
        id: String = "user-id",
        username: String = "alice",
        email: String = "alice@example.com",
        account: Account? = Account(password = "encoded-password"),
        roles: MutableList<Role> = mutableListOf(),
        active: Boolean = true,
    ) = User(
        id = id,
        username = username,
        email = email,
        name = username,
        account = account,
        roles = roles,
        active = active,
    )
}
