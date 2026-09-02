package com.gloomstone.clockin.iam.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpDto(
    @field:NotBlank @field:Size(min = 3, max = 100)
    val username: String,
    @field:NotBlank @field:Email @field:Size(max = 254)
    val email: String,
    @field:NotBlank @field:Size(min = 8, max = 200)
    val password: String,
    @field:NotBlank @field:Size(min = 8, max = 200)
    val passwordConfirm: String
)
