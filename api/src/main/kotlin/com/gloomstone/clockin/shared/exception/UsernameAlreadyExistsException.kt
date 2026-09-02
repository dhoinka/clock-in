package com.gloomstone.clockin.shared.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class UsernameAlreadyExistsException : RuntimeException("I guess we both know what's up")
