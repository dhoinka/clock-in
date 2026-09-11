package com.gloomstone.clockin.shared.exception
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
@RestControllerAdvice class GlobalExceptionHandler {
 @ExceptionHandler(BadRequestException::class) fun badRequest(e: BadRequestException)=response(HttpStatus.BAD_REQUEST,e.message)
 @ExceptionHandler(NotFoundException::class) fun notFound(e: NotFoundException)=response(HttpStatus.NOT_FOUND,e.message)
 @ExceptionHandler(ConflictException::class) fun conflict(e: ConflictException)=response(HttpStatus.CONFLICT,e.message)
 @ExceptionHandler(Exception::class) fun internal(e: Exception)=response(HttpStatus.INTERNAL_SERVER_ERROR,"An unexpected error occurred")
 private fun response(status:HttpStatus,message:String?)=ResponseEntity(ErrorResponse(status=status.value(),error=status.reasonPhrase,message=message,path=null),status)
}
