package com.gloomstone.clockin.iam.dto

data class PasswordRequestReset(
    val username: String,
    val password: String,
    val passwordRepeat: String
)