package com.gloomstone.clockin.iam.controller.mgmt

import com.gloomstone.clockin.iam.dto.CreateUserRequest
import com.gloomstone.clockin.iam.dto.PasswordRequestReset
import com.gloomstone.clockin.iam.dto.UpdateUserRequest
import com.gloomstone.clockin.iam.dto.UserDto
import com.gloomstone.clockin.iam.mapper.UserMapper
import com.gloomstone.clockin.iam.service.AuthService
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.exception.NotFoundException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Created by daniel on 03.05.2017.
 */
@RestController
@RequestMapping("/mgmt/users")
@PreAuthorize("hasAuthority('admin')")
class UserMgmtController(
    private val userService: UserService,
    private val authService: AuthService,
    private val userMapper: UserMapper
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun post(@RequestBody request: @Valid CreateUserRequest): UserDto {
        return userService.create(request)
            .let { userMapper.toDto(it) }
    }

    @PutMapping("/{id}")
    fun put(@PathVariable id: String, @RequestBody request: UpdateUserRequest): UserDto {
        return userService.update(id, request)
            .let { userMapper.toDto(it) }
    }


    @GetMapping
    fun get(): List<UserDto> {
        return userService.findAll().map { userMapper.toDto(it) }
    }

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: String): UserDto {
        return userService.findByIdentity(id)
            ?.let { userMapper.toDto(it) }
            ?: throw NotFoundException()
    }

    @DeleteMapping("/{userId}")
    fun delete(@PathVariable userId: String) {
        userService.delete(userId)
    }


    @PostMapping("/{userId}/reset-password")
    fun resetPassword(
        @PathVariable userId: String,
        @RequestBody request: PasswordRequestReset
    ) {
        authService.resetPassword(request, userId)
    }

}
