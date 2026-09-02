package com.gloomstone.clockin.iam.controller

import com.gloomstone.clockin.iam.dto.*
import com.gloomstone.clockin.iam.service.AuthService
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.exception.BadRequestException
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Created by daniel on 12.05.17.
 */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService,
) {
    @PostMapping("/login")
    fun login(@RequestBody request: @Valid LoginRequest?): Credentials {
        request ?: throw BadRequestException()
        return authService.authenticate(request.username, request.password)
    }

    @PostMapping("/refresh")
    fun refreshToken(@RequestBody request: RefreshRequest?): Credentials {
        request?.refreshToken ?: throw BadRequestException()
        return authService.refresh(request)
    }

    @PostMapping("/reset-password")
    @PreAuthorize("hasAuthority('admin')")
    fun resetPassword(@RequestBody request: PasswordRequestReset?, authentication: Authentication) {
        request ?: throw BadRequestException()
        if (authentication.authorities.contains(SimpleGrantedAuthority("admin"))) {
            authService.resetPassword(request, true)
        } else {
            authService.resetPassword(request)
        }
    }

    @PostMapping("/signup")
    fun signup(@RequestBody request: @Valid SignUpDto?): Credentials {
        request ?: throw BadRequestException()
        val user = userService.create(
            CreateUserRequest(
                username = request.username,
                email = request.email,
                name = request.username,
                active = true,
                password = request.password,
                passwordRepeat = request.passwordConfirm
            )
        )
        return authService.authenticate(user.username, request.password)
    }
}
