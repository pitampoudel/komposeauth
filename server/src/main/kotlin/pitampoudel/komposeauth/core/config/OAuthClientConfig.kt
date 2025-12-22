package pitampoudel.komposeauth.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import pitampoudel.komposeauth.app_config.service.AppConfigService

@Configuration
class OAuthClientConfig {

    @Bean
    fun clientRegistrationRepository(appConfigService: AppConfigService): ClientRegistrationRepository {
        val registrations = mutableListOf<ClientRegistration>()

        val webClientId = appConfigService.getConfig().googleAuthClientId
        val webClientSecret = appConfigService.getConfig().googleAuthClientSecret

        if (!webClientId.isNullOrBlank() && !webClientSecret.isNullOrBlank()) {
            registrations += CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(webClientId)
                .clientSecret(webClientSecret)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .build()
        }

        return if (registrations.isEmpty()) {
            ClientRegistrationRepository { null }
        } else {
            InMemoryClientRegistrationRepository(registrations)
        }
    }
}
