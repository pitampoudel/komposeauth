package pitampoudel.komposeauth.core.init

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.stereotype.Component
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.oauth_clients.entity.OAuth2Client
import pitampoudel.komposeauth.oauth_clients.repository.OAuth2ClientRepository

@Component
class SwaggerUiClientInitializer(
    val oauth2ClientRepository: OAuth2ClientRepository,
    val appConfigProvider: AppConfigProvider
) : ApplicationRunner {


    override fun run(args: ApplicationArguments) {
        val clientId = "swagger-ui"

        if (oauth2ClientRepository.existsById(clientId)) {
            return
        }

        val baseUrl = appConfigProvider.selfBaseUrl.trimEnd('/')
        val redirect = "$baseUrl/swagger-ui/oauth2-redirect.html"

        val client = OAuth2Client(
            clientId = clientId,
            clientSecret = null,
            clientName = "Swagger UI",
            clientAuthenticationMethods = setOf(ClientAuthenticationMethod.NONE),
            authorizationGrantTypes = setOf(
                AuthorizationGrantType.AUTHORIZATION_CODE,
                AuthorizationGrantType.REFRESH_TOKEN
            ),
            redirectUris = setOf(redirect),
            scopes = setOf("openid", "profile", "email"),
            // Require consent to be explicit in the demo; adjust if prefer auto-consent
            requireAuthorizationConsent = true,
            clientUri = null,
            logoUri = null
        )

        oauth2ClientRepository.save(client)
    }
}