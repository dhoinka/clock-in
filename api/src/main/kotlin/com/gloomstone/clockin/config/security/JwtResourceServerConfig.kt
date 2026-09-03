package com.gloomstone.clockin.config.security

import com.gloomstone.clockin.config.AppConfig
import com.gloomstone.clockin.shared.UserPrincipal
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.*
import java.nio.charset.StandardCharsets
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtResourceServerConfig(private val appConfig: AppConfig) {

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val secret = appConfig.jwtSecret ?: throw IllegalStateException("JWT secret is not configured")
        val key = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA512")

        val decoder = NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS512)
            .build()

        // Keep parity with JwtUtil.verifyToken(): issuer is required for access-token verification.
        val issuerValidator = JwtValidators.createDefaultWithIssuer("gloomstone.com")
        val audienceAndTypeValidator = OAuth2TokenValidator<Jwt> { jwt ->
            when {
                !jwt.audience.orEmpty().contains("gloomstone.com") -> OAuth2TokenValidatorResult.failure(
                    OAuth2Error("invalid_token", "Required audience is missing", null)
                )

                jwt.getClaimAsString("token_type") != "access" -> OAuth2TokenValidatorResult.failure(
                    OAuth2Error("invalid_token", "Token is not an access token", null)
                )

                else -> OAuth2TokenValidatorResult.success()
            }
        }
        decoder.setJwtValidator(DelegatingOAuth2TokenValidator(issuerValidator, audienceAndTypeValidator))
        return decoder
    }

    @Bean
    fun jwtAuthenticationConverter(): Converter<Jwt, out AbstractAuthenticationToken> {
        return Converter { jwt ->
            val user = jwt.getClaim<Map<String, Any?>>("user")
                ?: throw BadJwtException("Missing 'user' claim")

            val username = user["username"] as? String ?: throw BadJwtException("Missing username claim")
            val email = user["email"] as? String ?: throw BadJwtException("Missing email claim")
            val name = user["name"] as? String ?: throw BadJwtException("Missing name claim")

            val roles = (user["roles"] as? Collection<*>)
                ?.mapNotNull { it?.toString() }
                ?: emptyList()

            val principal = UserPrincipal(username, email, name, roles)
            val authorities = roles.map { SimpleGrantedAuthority(it) }

            UsernamePasswordAuthenticationToken(principal, jwt.tokenValue, authorities)
        }
    }
}
