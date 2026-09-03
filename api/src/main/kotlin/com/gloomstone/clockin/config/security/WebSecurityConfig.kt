package com.gloomstone.clockin.config.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


/**
 * Created by daniel on 30.08.2017.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class WebSecurityConfig {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        configuration.allowedOrigins = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun filterChain(
        http: HttpSecurity,
        corsConfigurationSource: CorsConfigurationSource,
        jwtAuthenticationConverter: Converter<Jwt, out AbstractAuthenticationToken>
    ): SecurityFilterChain {
        http
            .cors {
                it.configurationSource(corsConfigurationSource)
            }
            .csrf {
                it.disable()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling {
                it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .authorizeHttpRequests {
                // Public endpoints
                it.requestMatchers("/actuator/health").permitAll()
                it.requestMatchers("/", "/auth/login", "/auth/signup", "/auth/refresh").permitAll()
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // All other endpoints require authentication
                it.anyRequest().authenticated()
            }
            // This replaces the need for a manual JWTFilter.
            .oauth2ResourceServer { oauth2 ->
                oauth2.bearerTokenResolver(skipPublicEndpointsTokenResolver())
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }

        return http.build()
    }

    private fun skipPublicEndpointsTokenResolver(): (HttpServletRequest) -> String? {
        val defaultResolver = DefaultBearerTokenResolver()

        return { request ->
            // If the request is for a public route, pretend there is no token
            if (request.requestURI.startsWith("/auth") || request.requestURI.equals("/")
            ) {
                null
            } else {
                defaultResolver.resolve(request)
            }
        }
    }

}
