package com.gloomstone.clockin.shared.testutil

import com.gloomstone.clockin.shared.security.JwtUtil
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcConfigurer
import org.springframework.web.context.WebApplicationContext

// Top-level helpers (moved out of the companion object so they can be imported directly)
private const val DEFAULT_SECRET =
    "development-only-dummy-jwt-secret-change-before-deployment-0123456789abcdef"

fun jwtHeader(): MockMvcConfigurer {
    return object : MockMvcConfigurer {
        override fun afterConfigurerAdded(builder: ConfigurableMockMvcBuilder<*>) {
            // No-op
        }

        override fun beforeMockMvcCreated(
            builder: ConfigurableMockMvcBuilder<*>,
            context: WebApplicationContext
        ): RequestPostProcessor {
            return token()
        }
    }
}

fun token(): RequestPostProcessor {
    return token(
        username = "admin",
        email = "admin@example.org",
        name = "Admin",
        roles = listOf("admin", "manager")
    )
}

fun token(
    username: String,
    email: String,
    name: String,
    roles: List<String>
): RequestPostProcessor {
    return token(username, email, name, roles, DEFAULT_SECRET)
}

fun token(
    username: String,
    email: String,
    name: String,
    roles: List<String>,
    secret: String
): RequestPostProcessor {
    val jwtUtil = JwtUtil(secret)
    val userMap = mapOf(
        "username" to username,
        "email" to email,
        "name" to name,
        "roles" to roles
    )
    val tokenString = jwtUtil.generateAccessToken(userMap)

    return RequestPostProcessor { request ->
        request.addHeader("Authorization", "Bearer $tokenString")
        request
    }
}

fun token(username: String): RequestPostProcessor {
    return token(
        username = username,
        email = "$username@example.org",
        name = username,
        roles = emptyList()
    )
}
