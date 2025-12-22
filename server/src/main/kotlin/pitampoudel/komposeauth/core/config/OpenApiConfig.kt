package pitampoudel.komposeauth.core.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.StringSchema
import kotlinx.datetime.LocalDate
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pitampoudel.komposeauth.core.domain.ApiEndpoints
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
        return OpenAPI().info(
            Info().title("komposeauth API").description("The REST APIs")
        )
    }

    /**
     * Manually document non-MVC endpoints that are served by filters or static handlers.
     * SpringDoc scans only controller/functional endpoints, so we add those paths here.
     */
    @Bean
    fun openApiCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi: OpenAPI ->
        openApi.path(
            "/countries.json", PathItem().get(
                Operation()
                    .summary("Get countries")
                    .tags(listOf("static"))
            )
        )
        openApi.path(
            "/${ApiEndpoints.LOGOUT}", PathItem().get(
                Operation()
                    .summary("Logout the current user")
                    .tags(listOf("auth"))
            )
        )
        openApi.paths?.values?.forEach { pathItem ->
            pathItem.readOperations()?.forEach { operation ->
                operation.parameters?.removeIf { parameter ->
                    parameter.`in` == "query" && parameter.name == "securityContextRepository"
                }
            }
        }
    }
}
