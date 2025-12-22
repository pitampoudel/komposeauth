package pitampoudel.komposeauth

import io.sentry.Sentry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import java.util.TimeZone
import javax.annotation.PostConstruct

@SpringBootApplication
class AuthApplication(private val appConfigProvider: AppConfigProvider) {

    @PostConstruct
    fun init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        // initialize Sentry when DSN is provided
        val dsn = appConfigProvider.getConfig().sentryDsn
        if (dsn.isNullOrBlank()) return

        Sentry.init {
            it.dsn = dsn
        }
        Sentry.configureScope { scope ->
            scope.setTag("component", "Auth-Server")
            scope.setTag("environment", System.getenv("SPRING_PROFILES_ACTIVE") ?: "default")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<AuthApplication>(*args)
}
