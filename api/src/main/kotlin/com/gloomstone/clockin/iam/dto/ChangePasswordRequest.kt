package com.gloomstone.clockin.iam.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank @field:Size(max = 200)
    val oldPassword: String = "",
    @field:NotBlank @field:Size(min = 8, max = 200)
    val newPassword: String = "",
    @field:NotBlank @field:Size(min = 8, max = 200)
    val newPasswordConfirm: String = "",
)
