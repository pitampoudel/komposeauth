package pitampoudel.komposeauth.core.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter

@Configuration
class SerializationConfig {
    @Bean
    fun kotlinxJson(): Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
    }

    @Bean
    fun kotlinSerializationConverter(json: Json) = KotlinSerializationJsonHttpMessageConverter(json)

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.registerModule(JavaTimeModule())
        return mapper
    }
}