package com.gloomstone.clockin.iam.util

import org.mockito.invocation.InvocationOnMock
import java.security.MessageDigest

fun mockEncode(invocation: InvocationOnMock): String {
    val rawPassword = invocation.getArgument<String>(0)
    val bytes = MessageDigest.getInstance("SHA-256").digest(rawPassword.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun mockMatches(invocation: InvocationOnMock): Boolean {
    val rawPassword = invocation.getArgument<String>(0)
    val encodedPassword = invocation.getArgument<String>(1)

    // For existing database passwords that start with $2a$ (BCrypt), handle specially
    if (encodedPassword.startsWith("\$2a\$")) {
        // For the admin user with password "admin", this is the BCrypt hash
        return when {
            rawPassword == "admin" && encodedPassword == "\$2a\$10\$M6V/1XfMbWXKhKGvoMNtAemLKroYNHtHtY3gbpiOCs/n8jyZPKliO" -> true
            else -> false
        }
    }

    // For new passwords created in tests, use SHA-256 comparison
    val hashedRaw = MessageDigest.getInstance("SHA-256").digest(rawPassword.toByteArray())
    val hashedRawString = hashedRaw.joinToString("") { "%02x".format(it) }
    return hashedRawString == encodedPassword
}