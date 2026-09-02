package com.gloomstone.clockin.shared

/**
 * User principal containing authentication information.
 *
 * Represents the authenticated user with their username, email, name,
 * client/tenant identifier, and assigned roles.
 */
data class UserPrincipal(
    var username: String,
    var email: String,
    var name: String,
    var roles: List<String>
)