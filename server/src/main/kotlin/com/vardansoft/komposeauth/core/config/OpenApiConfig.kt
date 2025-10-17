package com.vardansoft.komposeauth.core.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import kotlinx.datetime.LocalDate
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
}
