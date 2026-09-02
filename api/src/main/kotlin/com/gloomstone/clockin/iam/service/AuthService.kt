package com.gloomstone.clockin.iam.service

import com.gloomstone.clockin.iam.dto.Credentials
import com.gloomstone.clockin.iam.dto.PasswordRequestReset
import com.gloomstone.clockin.iam.dto.RefreshRequest
import com.gloomstone.clockin.iam.mapper.UserMapper
import com.gloomstone.clockin.shared.exception.AuthenticationException
import com.gloomstone.clockin.shared.exception.BadRequestException
import com.gloomstone.clockin.shared.exception.InternalServerException
import com.gloomstone.clockin.shared.exception.NotFoundException
import com.gloomstone.clockin.shared.security.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Created by daniel on 13.05.17.
 */
@Service
class AuthService(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val userMapper: UserMapper,
    private val jwtUtil: JwtUtil,
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun authenticate(username: String, password: String): Credentials {
        try {
            val user = userService.findByIdentity(username.lowercase())
            val userPassword = if (user?.account?.password != null) {
                user.account?.password
            } else {
                // here we test against a dummy password to prevent timing attacks, see https://www.owasp.org/index.php/Authentication_Cheat_Sheet#Timing_Attack_Protection
                $$"$2a$10$fJBXxXlEEjYIi37lYvqZQ.TDoDDeKpK3r5qXtI4gJN5/8P04KXfbG"
            }

            if (passwordEncoder.matches(password, userPassword)) {
                if (user == null || !user.active) {
                    throw AuthenticationException()
                }

                val accessToken = jwtUtil.generateAccessToken(
                    mutableMapOf(
                        "username" to user.username,
                        "email" to user.email,
                        "name" to user.name,
                        "roles" to user.roles.map { it.name }.toTypedArray()
                    )
                )
                val refreshToken = jwtUtil.generateRefreshToken(user.username)

                this.userService.update(user)

                logger.info(
                    "Login successful, user=\"{}\"",
                    user.username,
                )
                return Credentials(userMapper.toDto(user), accessToken, refreshToken)
            }
        } catch (_: Exception) {

        }
        logger.warn("Login failed, user=\"{}\"", username)
        throw AuthenticationException()
    }

    @Transactional
    fun refresh(request: RefreshRequest): Credentials {
        val username = jwtUtil.verifyRefreshToken(request.refreshToken ?: throw BadRequestException()).name

        val user = this.userService.findByIdentity(username)
            ?.takeIf { it.active }
            ?: throw AuthenticationException()

        val accessToken = jwtUtil.generateAccessToken(
            mapOf(
                "username" to user.username,
                "email" to user.email,
                "name" to user.name,
                "roles" to user.roles.map { it.name }
            ))

        val refreshToken = jwtUtil.generateRefreshToken(user.username)

        this.userService.update(user)
        logger.info("User session refreshed, user=\"{}\"", user.username)
        return Credentials(userMapper.toDto(user), accessToken, refreshToken)
    }

    fun resetPassword(request: PasswordRequestReset, isAdmin: Boolean = false) {
        if (request.password != request.passwordRepeat) {
            throw Exception("Passwords do not match")
        }
        if (isAdmin) {
            val user = request.username.let { userService.findByIdentity(it) } ?: throw Exception("User not found")
            user.account ?: throw Exception("User has no account")
            user.account?.password = passwordEncoder.encode(request.password)
            userService.update(user)
        } else {
            val username = request.username
            val user = userService.findByIdentity(username) ?: throw Exception("User not found")
            user.account ?: throw Exception("User has no account")
            user.account?.password = passwordEncoder.encode(request.password)
            userService.update(user)
        }
    }

    fun resetPassword(request: PasswordRequestReset, userId: String) {
        val user = userService.findByIdentity(userId) ?: throw NotFoundException("User not found")
        if (request.password != request.passwordRepeat) {
            throw BadRequestException("Passwords do not match")
        }
        if (user.username != request.username) {
            throw BadRequestException("Cannot change password for other user")
        }
        user.account ?: throw InternalServerException("User has no account")
        user.account?.password = passwordEncoder.encode(request.password)
        userService.update(user)
    }
}
