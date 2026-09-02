package com.gloomstone.clockin.shared.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class PasswordEmptyException : RuntimeException()
