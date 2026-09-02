package com.gloomstone.clockin.shared.exception

/**
 * Exception for HTTP 400 Bad Request errors.
 */
class BadRequestException(message: String? = null) : RuntimeException(message)
