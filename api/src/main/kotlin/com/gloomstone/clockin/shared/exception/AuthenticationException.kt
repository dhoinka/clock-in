package com.gloomstone.clockin.shared.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
class AuthenticationException : RuntimeException("Wrong username or password")