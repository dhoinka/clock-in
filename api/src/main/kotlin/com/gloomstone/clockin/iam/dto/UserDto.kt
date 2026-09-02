package com.gloomstone.clockin.iam.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// Response DTO

data class UserDto(
    val id: String? = null,
    val username: String,
    val email: String,
    val name: String? = null,
    val active: Boolean? = null,
    val roles: List<String>? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

// Update request DTO

data class UpdateUserRequest(
    val username: String? = null,
    val email: String? = null,
    val name: String? = null,
    val active: Boolean? = null,
    val roles: List<String>? = null,
)

data class UpdateSelfRequest(
    @field:Size(min = 3, max = 100)
    val username: String? = null,
    @field:Email @field:Size(max = 254)
    val email: String? = null,
    @field:Size(max = 200)
    val name: String? = null,
)

// Create request DTO

data class CreateUserRequest(
    @field:Size(min = 3, max = 100)
    val username: String,
    @field:Email @field:Size(max = 254)
    val email: String,
    val name: String? = null,
    val active: Boolean? = null,
    val roles: List<String>? = null,

    @field:Size(min = 8, max = 200) val password: String,
    @field:Size(min = 8, max = 200) val passwordRepeat: String,
)
