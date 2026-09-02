package com.gloomstone.clockin.iam.service

import com.gloomstone.clockin.iam.controller.mgmt.dto.RoleDto
import com.gloomstone.clockin.iam.domain.Role
import com.gloomstone.clockin.iam.repository.RoleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RoleService(
    private val roleRepository: RoleRepository
) {
    fun find(): List<Role> {
        return roleRepository.findAll()
    }

    fun findById(id: Long): Role? {
        return roleRepository.findById(id).orElse(null)
    }

    fun create(dto: RoleDto): Role {
        val role = Role(dto.name ?: throw Exception("Name is required"))
        return roleRepository.save(role)
    }

    fun update(id: Long, dto: RoleDto): Role {
        val role = findById(id) ?: throw Exception("Role not found")
        return roleRepository.save(role)
    }

    fun remove(id: Long) {
        roleRepository.findById(id).ifPresent { entity: Role -> roleRepository.delete(entity) }
    }
}
