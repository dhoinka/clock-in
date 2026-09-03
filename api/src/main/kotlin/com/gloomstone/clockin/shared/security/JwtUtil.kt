package com.gloomstone.clockin.shared.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.gloomstone.clockin.shared.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * JWT utility for token generation and verification.
 * 
 * Handles creation and validation of access tokens, refresh tokens, and reset tokens.
 */
class JwtUtil(private val secret: String) {

    companion object {
        private val logger = LoggerFactory.getLogger(JwtUtil::class.java)
        private const val TOKEN_PREFIX = "Bearer"
        private const val ISSUER = "gloomstone.com"
        private const val TOKEN_TYPE = "token_type"
        private const val ACCESS_TOKEN = "access"
        private const val REFRESH_TOKEN = "refresh"
        private const val RESET_TOKEN = "reset"

        private fun accessTokenBuilder(): JWTCreator.Builder {
            return tokenBuilder()
                .withExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
        }

        private fun refreshTokenBuilder(): JWTCreator.Builder {
            return tokenBuilder()
                .withExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
        }

        private fun tokenBuilder(): JWTCreator.Builder {
            return JWT.create()
                // Generate a unique JWT ID (JTI) for token tracking and revocation
                // Replaces the previous unclear null claim with a random string
                .withJWTId(UUID.randomUUID().toString())
                .withIssuer(ISSUER)
                .withAudience(ISSUER)
                // Fixed: issuedAt should be current time, not 12 hours in the past
                // Previous behavior could cause token validation issues due to clock skew
                .withIssuedAt(Instant.now())
        }

    }

    fun verifyToken(token: String): UsernamePasswordAuthenticationToken {
        return try {
            val cleanToken = extractToken(token)

            val algorithm = Algorithm.HMAC512(secret)
            val verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .withAudience(ISSUER)
                .withClaim(TOKEN_TYPE, ACCESS_TOKEN)
                .build()
            val jwt = verifier.verify(cleanToken)

            val user = jwt.getClaim("user").asMap()

            val username = user["username"] as? String ?: throw IllegalArgumentException("Username claim is missing")
            val email = user["email"] as? String ?: throw IllegalArgumentException("Email claim is missing")
            val name = user["name"] as? String ?: throw IllegalArgumentException("Name claim is missing")

            @Suppress("UNCHECKED_CAST")
            val roles = user["roles"] as? List<String> ?: emptyList()

            val userPrincipal = UserPrincipal(username, email, name, roles)

            UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                roles.map { SimpleGrantedAuthority(it) }
            )
        } catch (e: Exception) {
            logger.error("Error verifying token: ${e.message}")
            throw e
        }
    }

    fun verifyRefreshToken(token: String): UsernamePasswordAuthenticationToken {
        val cleanToken = extractToken(token)

        val algorithm = Algorithm.HMAC512(secret)
        val verifier = JWT.require(algorithm)
            .withIssuer(ISSUER)
            .withAudience(ISSUER)
            .withClaim(TOKEN_TYPE, REFRESH_TOKEN)
            .build()
        val jwt = verifier.verify(cleanToken)

        val username = jwt.getClaim("username").asString()

        return UsernamePasswordAuthenticationToken(username, null)
    }

    fun generateRefreshToken(username: String): String {
        val algorithm = Algorithm.HMAC512(secret)

        return refreshTokenBuilder()
            .withClaim(TOKEN_TYPE, REFRESH_TOKEN)
            .withClaim("username", username)
            .sign(algorithm)
    }

    fun refreshRefreshToken(token: String): String {
        val algorithm = Algorithm.HMAC512(secret)
        val verifier = JWT.require(algorithm)
            .withIssuer(ISSUER)
            .withAudience(ISSUER)
            .withClaim(TOKEN_TYPE, REFRESH_TOKEN)
            .build()
        val jwt = verifier.verify(token)

        val username = jwt.getClaim("username").asString()

        return refreshTokenBuilder()
            .withClaim(TOKEN_TYPE, REFRESH_TOKEN)
            .withClaim("username", username)
            .sign(algorithm)
    }

    fun generateAccessToken(user: Map<String, *>): String {
        val algorithm = Algorithm.HMAC512(secret)

        return accessTokenBuilder()
            .withClaim(TOKEN_TYPE, ACCESS_TOKEN)
            .withClaim("user", user)
            .sign(algorithm)
    }

    fun generateResetToken(email: String): String {
        val algorithm = Algorithm.HMAC512(secret)
        return accessTokenBuilder()
            .withClaim(TOKEN_TYPE, RESET_TOKEN)
            .withClaim("email", email)
            .sign(algorithm)
    }

    fun verifyResetToken(token: String): String {
        val algorithm = Algorithm.HMAC512(secret)
        val verifier = JWT.require(algorithm)
            .withIssuer(ISSUER)
            .withAudience(ISSUER)
            .withClaim(TOKEN_TYPE, RESET_TOKEN)
            .build()
        val jwt = verifier.verify(token)
        return jwt.getClaim("email").asString()
    }

    private fun extractToken(token: String): String {
        return token.replace(TOKEN_PREFIX, "").trim()
    }
}
