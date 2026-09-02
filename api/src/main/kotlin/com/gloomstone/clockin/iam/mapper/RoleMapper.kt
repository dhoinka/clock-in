package com.gloomstone.clockin.iam.mapper

import com.gloomstone.clockin.iam.controller.mgmt.dto.RoleDto
import com.gloomstone.clockin.iam.domain.Role
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
interface RoleMapper {

    fun toDto(role: Role): RoleDto

    @Mapping(target = "users", ignore = true)
    fun toEntity(@MappingTarget role: Role, dto: RoleDto): Role
}
