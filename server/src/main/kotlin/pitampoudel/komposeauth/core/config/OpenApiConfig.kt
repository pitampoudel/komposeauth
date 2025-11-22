package pitampoudel.komposeauth.core.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.Scopes
import kotlinx.datetime.LocalDate
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Configuration
class OpenApiConfig {
    init {
        SpringDocUtils.getConfig()
            .replaceWithSchema(
                LocalDate::class.java,
                StringSchema().format("date").example("2024-01-15")
            )
            .replaceWithSchema(
                Instant::class.java,
                StringSchema().format("date-time").example("2024-01-15T12:00:00Z")
            )

    }

    @Bean
    fun openAPI(): OpenAPI {
        val oauth2SchemeName = "oauth2"
        return OpenAPI()
            .info(
                Info()
                    .title("komposeauth API")
                    .description("The REST APIs")
            )
            .addSecurityItem(
                SecurityRequirement().addList(oauth2SchemeName)
            )
            .components(
                Components()
                    // Enable Swagger UI "Authorize" with OAuth2 authorization code flow (PKCE-compatible)
                    .addSecuritySchemes(
                        oauth2SchemeName,
                        SecurityScheme()
                            .name(oauth2SchemeName)
                            .type(SecurityScheme.Type.OAUTH2)
                            .flows(
                                OAuthFlows()
                                    .authorizationCode(
                                        OAuthFlow()
                                            .authorizationUrl("/oauth2/authorize")
                                            .tokenUrl("/oauth2/token")
                                            .scopes(
                                                Scopes()
                                                    .addString("openid", "OpenID scope")
                                                    .addString("profile", "Basic profile information")
                                                    .addString("email", "Email address")
                                            )
                                    )
                            )
                    )
            )
    }

    /**
     * Manually document non-MVC endpoints that are served by filters or static handlers.
     * SpringDoc scans only controller/functional endpoints, so we add those paths here.
     */
    @Bean
    fun webauthnOpenApiCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi: OpenAPI ->
        openApi.path(
            "/countries.json", PathItem().get(
                Operation()
                    .summary("Get countries")
                    .tags(listOf("static"))
                    .description("Returns a list of countries")
            )
        )
        openApi.path(
            "/webauthn/register/options", PathItem().get(
                Operation()
                    .summary("Get WebAuthn register options")
                    .tags(listOf("webauthn"))
                    .description("Returns PublicKeyCredentialCreationOptions for WebAuthn registration")
            )
        )
    }
}
