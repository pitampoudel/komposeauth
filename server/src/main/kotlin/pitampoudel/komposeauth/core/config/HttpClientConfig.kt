package pitampoudel.komposeauth.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.http.HttpClient
import java.time.Duration

/**
 * Configuration for HTTP client with connection pooling and timeout settings.
 * Improves performance for external API calls like Google OAuth.
 */
@Configuration
class HttpClientConfig {

    @Bean
    fun httpClient(): HttpClient {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build()
    }
}
