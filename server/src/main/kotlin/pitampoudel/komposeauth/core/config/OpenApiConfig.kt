package pitampoudel.komposeauth.core.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
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
        val securitySchemeName = "auth-jwt"
        return OpenAPI()
            .info(
                Info()
                    .title("komposeauth API")
                    .description("The REST APIs")
            )
            .addSecurityItem(
                SecurityRequirement().addList(securitySchemeName)
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
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

        openApi.path(
            "/v3/api-docs", PathItem().get(
                Operation()
                    .summary("Get OpenAPI JSON")
                    .tags(listOf("docs"))
            )
        )
    }
}
