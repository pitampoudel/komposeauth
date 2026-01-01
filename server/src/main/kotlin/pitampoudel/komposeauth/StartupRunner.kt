package pitampoudel.komposeauth

import io.sentry.Sentry
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.migrations.DbMigrationRunner

@Component
class StartupRunner(
    val appConfigService: AppConfigService,
    private val dbMigrationRunner: DbMigrationRunner
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val dsn = appConfigService.getConfig().sentryDsn
        if (!dsn.isNullOrBlank()) {
            Sentry.init { options ->
                options.dsn = dsn
            }
        }
        dbMigrationRunner.migrateIfNeeded()

    }
}
