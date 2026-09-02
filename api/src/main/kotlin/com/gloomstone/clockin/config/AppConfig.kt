package com.gloomstone.clockin.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("app")
class AppConfig {
    var jwtSecret: String? = null

}
