package com.gloomstone.clockin.shared.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.gloomstone.clockin.shared.UserPrincipal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JwtUtilTest {

    private lateinit var jwtUtil: JwtUtil
    private val secret = "secret"
    private val issuer = "gloomstone.com"

    @BeforeEach
    fun setUp() {
        jwtUtil = JwtUtil(secret)
    }

    @Test
    fun verifyToken() {
        val userMap = mapOf(
            "username" to "testUser",
            "email" to "test@example.com",
            "name" to "Test User",
            "roles" to listOf("ROLE_USER")
        )

        val token = jwtUtil.generateAccessToken(userMap)
        val authentication = jwtUtil.verifyToken(token)

        assertThat(authentication).isNotNull

        val userPrincipal = authentication.principal as UserPrincipal

        assertThat(userPrincipal.username).isEqualTo("testUser")
        assertThat(userPrincipal.name).isEqualTo("Test User")
        assertThat(userPrincipal.email).isEqualTo("test@example.com")

        assertThat(
            authentication.authorities.any { it.authority == "ROLE_USER" }
        ).isTrue()
    }

    @Test
    fun verifyRefreshToken() {
        val token = jwtUtil.generateRefreshToken("testUser", "session-id")
        val claims = jwtUtil.verifyRefreshToken(token.value)

        assertThat(claims.username).isEqualTo("testUser")
        assertThat(claims.sessionId).isEqualTo("session-id")
        assertThat(claims.jti).isEqualTo(token.jti)
    }

    @Test
    fun generateRefreshToken() {
        val token = jwtUtil.generateRefreshToken("testUser", "session-id")
        assertThat(token.value).isNotBlank()

        val algorithm = Algorithm.HMAC512(secret)
        assertDoesNotThrow {
            JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(issuer)
                .build()
                .verify(token.value)
        }
    }

    @Test
    fun generateResetToken() {
        val token = jwtUtil.generateResetToken("test@example.com")
        assertThat(token).isNotNull

        val algorithm = Algorithm.HMAC512(secret)
        assertDoesNotThrow {
            JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(issuer)
                .build()
                .verify(token)
        }
    }

    @Test
    fun verifyResetToken() {
        val token = jwtUtil.generateResetToken("test@example.com")
        val email = jwtUtil.verifyResetToken(token)

        assertThat(email).isNotNull
        assertThat(email).isEqualTo("test@example.com")
    }
}
