package com.gloomstone.clockin.iam.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Created by daniel on 13.05.17.
 */
data class LoginRequest(
    @field:NotBlank
    @field:Size(max = 254)
    val username: String,
    @field:NotBlank
    @field:Size(max = 200)
    val password: String,
)
