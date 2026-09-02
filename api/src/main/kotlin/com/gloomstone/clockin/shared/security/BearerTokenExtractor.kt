package com.gloomstone.clockin.shared.security

import jakarta.servlet.http.HttpServletRequest
import java.util.*

/**
 * Utility for extracting Bearer tokens from Authorization headers.
 * 
 * This class provides a single responsibility: extract JWT tokens from
 * HTTP Authorization headers following RFC 6750 standards.
 * 
 * Usage:
 * ```kotlin
 * val token = extract(request)
 *     .orElse(null)
 * ```
 */
const val BEARER_PREFIX = "Bearer "
private const val MIN_TOKEN_LENGTH = 10 // Arbitrary but reasonable minimum

/**
 * Extracts Bearer token from Authorization header.
 *
 * @param request HttpServletRequest containing Authorization header
 * @return Optional containing token if present and valid, empty otherwise
 */
fun extractToken(request: HttpServletRequest): Optional<String> {
    val authHeader = request.getHeader("Authorization")
    return extractToken(authHeader)
}

/**
 * Extracts Bearer token from Authorization header string.
 *
 * @param authHeader Authorization header value (e.g., "Bearer <token>")
 * @return Optional containing token if present and valid, empty otherwise
 */
fun extractToken(authHeader: String?): Optional<String> {
    if (authHeader.isNullOrBlank()) {
        return Optional.empty()
    }

    if (!authHeader.startsWith(BEARER_PREFIX)) {
        return Optional.empty()
    }

    val token = authHeader.substring(BEARER_PREFIX.length).trim()

    if (token.isBlank() || token.length < MIN_TOKEN_LENGTH) {
        return Optional.empty()
    }

    return Optional.of(token)
}
