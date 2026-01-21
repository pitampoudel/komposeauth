package pitampoudel.komposeauth.core.config

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig(private val json: Json) {

    @Bean
    fun restClient(builder: RestClient.Builder): RestClient {
        return builder.configureMessageConverters { converters ->
            converters.withKotlinSerializationJsonConverter(
                KotlinSerializationJsonHttpMessageConverter(json)
            )
        }.build()
    }
}