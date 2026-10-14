package pitampoudel.komposeauth

import io.sentry.Sentry
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import pitampoudel.komposeauth.app_config.service.AppConfigService

@Component
class StartupRunner(
    val appConfigService: AppConfigService
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        applyErrorReporting(appConfigService.getConfig().sentryDsn)
    }

    /**
     * Starts or stops error reporting for [dsn]. Also called when the DSN is saved, so a change takes
     * effect on the instance that saved it without a restart; the others pick it up when they next start.
     */
    fun applyErrorReporting(dsn: String?) {
        if (dsn.isNullOrBlank()) {
            if (Sentry.isEnabled()) Sentry.close()
            return
        }
        Sentry.init { options ->
            options.dsn = dsn
        }
    }
}
