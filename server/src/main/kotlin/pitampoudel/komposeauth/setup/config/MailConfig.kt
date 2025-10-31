package pitampoudel.komposeauth.setup.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import pitampoudel.komposeauth.AppProperties

@Configuration
class MailConfig(private val appProperties: AppProperties) {
    @Bean
    fun javaMailSender(): JavaMailSender? {
        val impl = JavaMailSenderImpl()
        impl.host = appProperties.smtpHost
        impl.port = appProperties.smtpPort ?: 587
        impl.username = appProperties.smtpUsername
        impl.password = appProperties.smtpPassword

        val props = impl.javaMailProperties
        props["mail.smtp.from"] = appProperties.smtpFromEmail
        props["mail.smtp.auth"] = !appProperties.smtpUsername.isNullOrBlank()
        props["mail.smtp.starttls.enable"] = "true"
        return impl
    }
}