package com.gloomstone.clockin.iam.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PasswordRequestReset(
    val username: String,
    @field:NotBlank @field:Size(min = 8, max = 200)
    val password: String,
    @field:NotBlank @field:Size(min = 8, max = 200)
    val passwordRepeat: String
)
