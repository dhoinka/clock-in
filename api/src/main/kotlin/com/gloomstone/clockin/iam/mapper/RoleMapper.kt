package com.gloomstone.clockin.iam.mapper

import com.gloomstone.clockin.iam.controller.mgmt.dto.RoleDto
import com.gloomstone.clockin.iam.domain.Role
import org.springframework.stereotype.Component

@Component
class RoleMapper {
    fun toDto(role: Role) = RoleDto(id = role.id, name = role.name)

    fun toEntity(role: Role, dto: RoleDto): Role = role.apply {
        dto.id?.let { id = it }
        dto.name?.let { name = it }
    }
}
