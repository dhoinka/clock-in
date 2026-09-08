package com.gloomstone.clockin.config

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.DeserializationFeature


@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties
class Config {

    @Bean
    fun restTemplate(): RestTemplate {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(3_000)
            setReadTimeout(5_000)
        }
        return RestTemplate(requestFactory)
    }

    @Bean
    fun jacksonCustomizer(): JsonMapperBuilderCustomizer =
        JsonMapperBuilderCustomizer { builder ->
            builder.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            builder.changeDefaultPropertyInclusion { old ->
                old.withValueInclusion(JsonInclude.Include.NON_NULL)
            }

        }


}
