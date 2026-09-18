package com.gloomstone.clockin.shared.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BadRequestException::class)
    fun badRequest(exception: BadRequestException): ResponseEntity<ErrorResponse> {
        return response(HttpStatus.BAD_REQUEST, exception.message)
    }

    @ExceptionHandler(NotFoundException::class, NoResourceFoundException::class)
    fun notFound(exception: Exception): ResponseEntity<ErrorResponse> {
        return response(HttpStatus.NOT_FOUND, exception.message)
    }

    @ExceptionHandler(ConflictException::class)
    fun conflict(exception: ConflictException): ResponseEntity<ErrorResponse> {
        return response(HttpStatus.CONFLICT, exception.message)
    }

    @ExceptionHandler(Exception::class)
    fun internal(exception: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception while processing request", exception)
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
    }

    private fun response(status: HttpStatus, message: String?): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = null,
        )
        return ResponseEntity(body, status)
    }
}
