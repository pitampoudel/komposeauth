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
        ignoreUnknownKeys = true
    }

    @Bean
    fun kotlinSerializationConverter(json: Json) = KotlinSerializationJsonHttpMessageConverter(json)


    @Bean
    fun cborObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper(CBORFactory())
        mapper.registerModule(JavaTimeModule())
        return mapper
    }

    /**
     * Also writes and reads Spring's WebAuthn types in the WebAuthn JSON shape (a base64url
     * `challenge`, string enums), which their own fields don't say. It is the Jackson 2 module,
     * deprecated beside the Jackson 3 one, because WebAuthn4J's [ObjectConverter] is built over this
     * mapper and speaks Jackson 2.
     */
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.registerModule(JavaTimeModule())
        @Suppress("DEPRECATION")
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