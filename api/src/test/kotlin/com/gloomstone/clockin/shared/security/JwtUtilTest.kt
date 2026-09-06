package com.gloomstone.clockin.shared.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.gloomstone.clockin.shared.UserPrincipal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

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
    fun `reset token lifetime is independent from access token lifetime`() {
        val shortAccessTokenJwtUtil = JwtUtil(
            secret,
            accessTokenLifetime = Duration.ofSeconds(10),
            resetTokenLifetime = Duration.ofHours(1),
        )

        val resetToken = JWT.decode(shortAccessTokenJwtUtil.generateResetToken("test@example.com"))

        assertThat(Duration.between(resetToken.issuedAtAsInstant, resetToken.expiresAtAsInstant))
            .isEqualTo(Duration.ofHours(1))
    }

    @Test
    fun verifyResetToken() {
        val token = jwtUtil.generateResetToken("test@example.com")
        val email = jwtUtil.verifyResetToken(token)

        assertThat(email).isNotNull
        assertThat(email).isEqualTo("test@example.com")
    }
}
