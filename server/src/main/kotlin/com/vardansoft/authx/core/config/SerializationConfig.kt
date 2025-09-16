package com.vardansoft.authx.core.config

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter

@Configuration
class SerializationConfig {
    @Bean
    fun kotlinSerializationConverter() = KotlinSerializationJsonHttpMessageConverter(
        Json {
            classDiscriminator = "type"
        }
    )

}