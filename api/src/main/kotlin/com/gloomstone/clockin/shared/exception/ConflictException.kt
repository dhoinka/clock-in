package com.gloomstone.clockin.shared.exception

/**
 * Exception for HTTP 409 Conflict errors.
 */
class ConflictException(message: String? = null) : RuntimeException(message)

