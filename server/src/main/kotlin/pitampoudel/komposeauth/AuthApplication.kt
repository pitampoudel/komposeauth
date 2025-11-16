package pitampoudel.komposeauth

import io.sentry.Sentry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import javax.annotation.PostConstruct

@SpringBootApplication
class AuthApplication(private val appProperties: AppProperties) {

    @PostConstruct
    fun initSentry() {
        // initialize Sentry when DSN is provided
        val dsn = appProperties.sentryDsn
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
