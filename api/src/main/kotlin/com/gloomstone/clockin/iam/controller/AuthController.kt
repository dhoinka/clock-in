package com.gloomstone.clockin.iam.controller

import com.gloomstone.clockin.config.AppConfig
import com.gloomstone.clockin.iam.dto.*
import com.gloomstone.clockin.iam.service.AuthService
import com.gloomstone.clockin.iam.service.UserService
import com.gloomstone.clockin.shared.exception.AuthenticationException
import com.gloomstone.clockin.shared.exception.BadRequestException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.*

/**
 * Created by daniel on 12.05.17.
 */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService,
    private val appConfig: AppConfig,
) {
    @PostMapping("/login")
    fun login(@RequestBody request: @Valid LoginRequest?): ResponseEntity<Credentials> {
        request ?: throw BadRequestException()
        return withRefreshCookie(authService.authenticate(request.username, request.password))
    }

    @PostMapping("/refresh")
    fun refreshToken(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Credentials> {
        logger.info("Incoming refresh token request")
        val refreshToken = request.cookies?.firstOrNull { it.name == REFRESH_COOKIE }?.value
        if (refreshToken == null) {
            logger.warn("Refresh rejected: refresh cookie is missing")
            clearRefreshCookie(response)
            throw AuthenticationException()
        }
        return try {
            withRefreshCookie(authService.refresh(refreshToken))
        } catch (exception: AuthenticationException) {
            clearRefreshCookie(response)
            throw exception
        }
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Void> {
        authService.logout(request.cookies?.firstOrNull { it.name == REFRESH_COOKIE }?.value)
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, refreshCookie("").maxAge(0).build().toString())
            .build()
    }

    @GetMapping("/session")
    fun session(request: HttpServletRequest): SessionStatus {
        return SessionStatus(
            hasRefreshCookie = request.cookies?.any { it.name == REFRESH_COOKIE } == true,
        )
    }

    @PostMapping("/reset-password")
    @PreAuthorize("hasAuthority('admin')")
    fun resetPassword(@Valid @RequestBody request: PasswordRequestReset?, authentication: Authentication) {
        request ?: throw BadRequestException()
        if (authentication.authorities.contains(SimpleGrantedAuthority("admin"))) {
            authService.resetPassword(request, true)
        } else {
            authService.resetPassword(request)
        }
    }

    @PostMapping("/signup")
    fun signup(@RequestBody request: @Valid SignUpDto?): ResponseEntity<Credentials> {
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
        return withRefreshCookie(authService.authenticate(user.username, request.password))
    }

    private fun withRefreshCookie(credentials: Credentials): ResponseEntity<Credentials> =
        ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie(credentials.refreshToken).build().toString())
            .body(credentials)

    private fun refreshCookie(value: String): ResponseCookie.ResponseCookieBuilder = ResponseCookie
        .from(REFRESH_COOKIE, value)
        .httpOnly(true)
        .secure(appConfig.refreshCookieSecure)
        .sameSite("Strict")
        // Traefik removes /api before forwarding, but browsers retain it for cookie matching.
        .path("/api/auth")
        .maxAge(appConfig.refreshTokenLifetime)

    private fun clearRefreshCookie(response: HttpServletResponse) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("").maxAge(0).build().toString())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthController::class.java)
        private const val REFRESH_COOKIE = "refresh_token"
    }
}

data class SessionStatus(val hasRefreshCookie: Boolean)
