package pitampoudel.komposeauth.core.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.webauthn4j.converter.util.ObjectConverter
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module

@Configuration
class SerializationConfig {
    @Bean
    fun kotlinxJson(): Json = Json {
        classDiscriminator = "type"
    }

    @Bean
    fun kotlinSerializationConverter(json: Json) = KotlinSerializationJsonHttpMessageConverter(json)


    @Bean
    fun cborObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper(CBORFactory())
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(WebauthnJackson2Module())
        return mapper
    }

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(WebauthnJackson2Module())
        return mapper
    }

    @Bean
    fun objectConverter(
        objectMapper: ObjectMapper,
        @Qualifier("cborObjectMapper")
        cborObjectMapper: ObjectMapper
    ): ObjectConverter = ObjectConverter(
        objectMapper,
        cborObjectMapper
    )
}