package com.gloomstone.clockin.iam.controller.mgmt

import com.gloomstone.clockin.iam.controller.mgmt.dto.RoleDto
import com.gloomstone.clockin.iam.mapper.RoleMapper
import com.gloomstone.clockin.iam.service.RoleService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/mgmt")
@PreAuthorize("hasAuthority('admin')")
class RoleMgmtController(
    private val roleService: RoleService,
    private val roleMapper: RoleMapper
) {
    @PostMapping("/roles")
    fun post(@RequestBody request: RoleDto?): RoleDto {
        request ?: throw Exception("Role not found")
        return roleService.create(request)
            .let { roleMapper.toDto(it) }
    }

    @PutMapping("/roles/{id}")
    fun put(@PathVariable id: Long, @RequestBody request: RoleDto): RoleDto {
        return roleService.update(id, request)
            .let { roleMapper.toDto(it) }
    }

    @DeleteMapping("/roles/{id}")
    fun delete(@PathVariable id: Long) {
        roleService.remove(id)
    }

    @GetMapping("/roles")
    fun get(): List<RoleDto> {
        return roleService.find()
            .map { roleMapper.toDto(it) }
    }

    @GetMapping("/roles/{id}")
    fun getOne(@PathVariable id: Long): RoleDto {
        return roleService.findById(id)
            ?.let { roleMapper.toDto(it) }
            ?: throw Exception("Role not found")
    }
}
