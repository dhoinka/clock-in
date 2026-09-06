package com.gloomstone.clockin.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConfigurationProperties("app")
class AppConfig {
    var jwtSecret: String? = null
    var accessTokenLifetime: Duration = Duration.ofMinutes(15)
    var resetTokenLifetime: Duration = Duration.ofHours(1)
    var refreshTokenLifetime: Duration = Duration.ofDays(7)

    /** Defaults to secure cookies; local HTTP development may explicitly disable it. */
    var refreshCookieSecure: Boolean = true

}
