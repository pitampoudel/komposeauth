package pitampoudel.komposeauth.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.utils.ServerUrl

@Configuration
class OAuthClientConfig {

    /**
     * Read from the config on every lookup rather than once at boot: the Google credentials are set
     * on the config page of an already running server, and a registration captured at startup left
     * the login page sending people to Google with a client that did not exist until a restart.
     *
     * The redirect URI is the configured address rather than Spring's `{baseUrl}`, which is worked
     * out from each request's Host and forwarded headers.
     */
    @Bean
    fun clientRegistrationRepository(appConfigService: AppConfigService, serverUrl: ServerUrl) =
        ClientRegistrationRepository { registrationId ->
            val config = appConfigService.getConfig()
            val clientId = config.googleAuthClientId
            val clientSecret = config.googleAuthClientSecret
            if (registrationId != GOOGLE || clientId.isNullOrBlank() || clientSecret.isNullOrBlank()) {
                null
            } else {
                CommonOAuth2Provider.GOOGLE.getBuilder(GOOGLE)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .redirectUri("${serverUrl.value}/{action}/oauth2/code/{registrationId}")
                    .build()
            }
        }

    @Bean
    fun authorizationRequestResolver(
        clientRegistrationRepository: ClientRegistrationRepository
    ): OAuth2AuthorizationRequestResolver = GoogleAuthorizationRequestResolver(
        DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository,
            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
        )
    )

    private companion object {
        const val GOOGLE = "google"
    }
}
