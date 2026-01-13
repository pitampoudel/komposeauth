package pitampoudel.komposeauth

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.thymeleaf.TemplateEngine
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.service.EmailService

@TestConfiguration(proxyBeanMethods = false)
class NoopEmailTestConfig {
    @Bean
    @Primary
    fun noopEmailService(
        appConfigService: AppConfigService,
        templateEngine: TemplateEngine
    ): EmailService = object : EmailService(appConfigService, templateEngine) {
        override fun sendHtmlMail(
            baseUrl: String,
            to: String,
            subject: String,
            template: String,
            model: Map<String, Any?>
        ): Boolean = true
    }
}

