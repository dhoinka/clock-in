package com.gloomstone.clockin.iam.mapper

import com.gloomstone.clockin.iam.domain.Role
import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.UpdateSelfRequest
import com.gloomstone.clockin.iam.dto.UpdateUserRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UserMapperTest {
    private val mapper = UserMapper()

    @Test
    fun `maps a user to its public representation`() {
        val user = User("alice@example.com", "alice", "Alice", roles = mutableListOf(Role("admin")))

        val result = mapper.toDto(user)

        assertThat(result.username).isEqualTo("alice")
        assertThat(result.email).isEqualTo("alice@example.com")
        assertThat(result.roles).containsExactly("admin")
    }

    @Test
    fun `management update changes only supplied fields`() {
        val user = User("old@example.com", "alice", "Alice", active = true)

        mapper.update(UpdateUserRequest(email = "new@example.com"), user)

        assertThat(user.email).isEqualTo("new@example.com")
        assertThat(user.username).isEqualTo("alice")
        assertThat(user.name).isEqualTo("Alice")
        assertThat(user.active).isTrue()
    }

    @Test
    fun `self update cannot change privileged fields`() {
        val role = Role("admin")
        val user = User("old@example.com", "alice", "Alice", roles = mutableListOf(role), active = true)

        mapper.updateSelf(UpdateSelfRequest(name = "Alice Smith"), user)

        assertThat(user.name).isEqualTo("Alice Smith")
        assertThat(user.active).isTrue()
        assertThat(user.roles).containsExactly(role)
    }
}
