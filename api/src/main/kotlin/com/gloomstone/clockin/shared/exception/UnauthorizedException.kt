package com.gloomstone.clockin.shared.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Exception for HTTP 401 Unauthorized errors.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthorizedException(message: String? = "Unauthorized") : RuntimeException(message)

