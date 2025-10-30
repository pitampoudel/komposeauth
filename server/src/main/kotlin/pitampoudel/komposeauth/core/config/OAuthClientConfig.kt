package pitampoudel.komposeauth.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import pitampoudel.komposeauth.AppProperties

@Configuration
class OAuthClientConfig {

    @Bean
    fun clientRegistrationRepository(appProperties: AppProperties): ClientRegistrationRepository? {
        val registrations = mutableListOf<ClientRegistration>()

        val webClientId = appProperties.googleAuthClientId
        val webClientSecret = appProperties.googleAuthClientSecret

        if (!webClientId.isNullOrBlank() && !webClientSecret.isNullOrBlank()) {
            registrations += CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(webClientId)
                .clientSecret(webClientSecret)
                .scope("openid", "profile", "email")
                .build()
        }

        if (registrations.isEmpty()) return null

        return InMemoryClientRegistrationRepository(registrations)
    }
}
