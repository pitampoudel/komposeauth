package com.vardansoft.authx

import com.vardansoft.authx.core.utils.GcpUtils
import io.sentry.Sentry
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class AuthApplication {

    @Bean
    fun startupChecks(appProperties: AppProperties): ApplicationRunner = ApplicationRunner {
        GcpUtils.assertAuthenticatedProject(appProperties.expectedGcpProjectId)
        Sentry.init { options ->
            options.dsn = appProperties.sentryDsn
        }
    }
}

fun main(args: Array<String>) {
    runApplication<AuthApplication>(*args)
}