package com.gloomstone.clockin.iam.dto

data class Credentials(val user: UserDto, val accessToken: String, val refreshToken: String)
