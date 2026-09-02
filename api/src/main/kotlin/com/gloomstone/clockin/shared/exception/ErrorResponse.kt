package com.gloomstone.clockin.shared.exception

import java.time.LocalDateTime

/**
 * Standard error response schema for API errors.
 */
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String?,
    val path: String?
)

