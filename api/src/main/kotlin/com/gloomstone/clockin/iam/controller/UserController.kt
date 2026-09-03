package com.gloomstone.clockin.iam.controller

import com.gloomstone.clockin.iam.dto.ChangePasswordRequest
import com.gloomstone.clockin.iam.dto.UpdateSelfRequest
import com.gloomstone.clockin.iam.dto.UserDto
import com.gloomstone.clockin.iam.mapper.UserMapper
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.UserPrincipal
import com.gloomstone.clockin.shared.exception.NotFoundException
import com.gloomstone.clockin.shared.exception.PasswordNotEqualException
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
class UserController(private val userService: UserService, private val userMapper: UserMapper) {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('admin')")
    fun get(@PathVariable id: String): UserDto {
        return getOne(id)
    }

    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal user: UserPrincipal): UserDto {
        return getOne(user.username)
    }

    @PutMapping("/me")
    fun put(
        @RequestBody request: @Valid UpdateSelfRequest,
        @AuthenticationPrincipal user: UserPrincipal
    ): UserDto {
        val originUser = userService.findByIdentity(user.username) ?: throw NotFoundException()

        val updatedUser = userService.updateSelf(originUser.id, request)
        logger.debug("User updated: {}", updatedUser)
        return userMapper.toDto(updatedUser)
    }

    @RequestMapping(value = ["/me/password"], method = [RequestMethod.PUT, RequestMethod.POST])
    fun changePassword(
        @RequestBody passwordRequest: @Valid ChangePasswordRequest,
        @AuthenticationPrincipal user: UserPrincipal
    ): UserDto {
        if (passwordRequest.newPassword != passwordRequest.newPasswordConfirm) {
            throw PasswordNotEqualException()
        }
        val originUser = userService.findByIdentity(user.username) ?: throw NotFoundException()
        logger.debug("User: {}", originUser)
        return userMapper.toDto(
            userService.changePassword(originUser, passwordRequest.oldPassword, passwordRequest.newPassword)
        )
    }

    private fun getOne(value: String): UserDto {
        val user = userService.findByIdentity(value)
            ?: throw NotFoundException()
        return userMapper.toDto(user)
    }
}
