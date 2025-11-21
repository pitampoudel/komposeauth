package pitampoudel.komposeauth.core.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.config.service.AppConfigProvider

@Service
class EmailService(private val appConfigProvider: AppConfigProvider) {

    fun javaMailSender(): JavaMailSender {
        val impl = JavaMailSenderImpl()
        impl.host = appConfigProvider.smtpHost
        impl.port = appConfigProvider.smtpPort ?: 587
        impl.username = appConfigProvider.smtpUsername
        impl.password = appConfigProvider.smtpPassword

        val props = impl.javaMailProperties
        props["mail.smtp.from"] = appConfigProvider.smtpFromEmail
        props["mail.smtp.auth"] = !appConfigProvider.smtpUsername.isNullOrBlank()
        props["mail.smtp.starttls.enable"] = "true"
        return impl
    }

    fun sendSimpleMail(
        to: String,
        subject: String? = null,
        text: String? = null
    ): Boolean {
        return try {
            val mailMessage = SimpleMailMessage()
            mailMessage.setTo(to)
            mailMessage.text = text
            mailMessage.subject = subject
            javaMailSender().send(mailMessage)
            true
        } catch (e: Exception) {
            false
        }
    }
}