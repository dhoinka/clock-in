package com.gloomstone.clockin.iam.mapper

import com.gloomstone.clockin.iam.domain.Role
import com.gloomstone.clockin.iam.domain.User
import com.gloomstone.clockin.iam.dto.UpdateSelfRequest
import com.gloomstone.clockin.iam.dto.UpdateUserRequest
import com.gloomstone.clockin.iam.dto.UserDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
)
interface UserMapper {
    fun toDto(user: User): UserDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "roles", ignore = true)
    fun toEntity(userDto: UserDto): User

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    fun update(userMgmtDto: UpdateUserRequest, @MappingTarget user: User)

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    fun updateSelf(request: UpdateSelfRequest, @MappingTarget user: User)


    fun rolesToString(roles: List<Role>): List<String>

    fun roleToString(role: Role): String {
        return role.name
    }
}
