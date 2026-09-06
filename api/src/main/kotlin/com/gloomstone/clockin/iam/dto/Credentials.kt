package com.gloomstone.clockin.iam.dto

import com.fasterxml.jackson.annotation.JsonIgnore

data class Credentials(val user: UserDto, val accessToken: String, @get:JsonIgnore val refreshToken: String)
