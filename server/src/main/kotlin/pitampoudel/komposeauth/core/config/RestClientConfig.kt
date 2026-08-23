package pitampoudel.komposeauth.core.config

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class RestClientConfig(private val json: Json) {

    /**
     * Slack and the third-party KYC provider are reached through this, and neither is on a path
     * that can afford to wait forever: with no timeout, a provider that accepts the connection and
     * then goes quiet pins the request thread permanently, and the request that triggered it never
     * completes.
     */
    @Bean
    fun restClient(builder: RestClient.Builder): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(10))
            setReadTimeout(Duration.ofSeconds(15))
        }
        return builder
            .requestFactory(requestFactory)
            .configureMessageConverters { converters ->
                converters.withKotlinSerializationJsonConverter(
                    KotlinSerializationJsonHttpMessageConverter(json)
                )
            }.build()
    }
}