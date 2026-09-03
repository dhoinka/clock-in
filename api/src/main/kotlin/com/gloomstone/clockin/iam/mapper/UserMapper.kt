package com.gloomstone.clockin.iam.mapper

import com.gloomstone.clockin.iam.domain.Role
import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.UpdateSelfRequest
import com.gloomstone.clockin.iam.dto.UpdateUserRequest
import com.gloomstone.clockin.iam.dto.UserDto
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class UserMapper {
    fun toDto(user: User) = UserDto(
        id = user.id,
        username = user.username,
        email = user.email,
        name = user.name,
        active = user.active,
        roles = user.roles.map(Role::name),
        createdAt = user.createdAt,
        updatedAt = user.updatedAt,
    )

    fun toEntity(userDto: UserDto) = User(
        email = userDto.email,
        username = userDto.username,
        name = userDto.name.orEmpty(),
        active = userDto.active ?: false,
        createdAt = userDto.createdAt ?: LocalDateTime.now(),
        updatedAt = userDto.updatedAt ?: LocalDateTime.now(),
    )

    fun update(request: UpdateUserRequest, user: User) {
        request.username?.let { user.username = it }
        request.email?.let { user.email = it }
        request.name?.let { user.name = it }
        request.active?.let { user.active = it }
    }

    fun updateSelf(request: UpdateSelfRequest, user: User) {
        request.username?.let { user.username = it }
        request.email?.let { user.email = it }
        request.name?.let { user.name = it }
    }
}
