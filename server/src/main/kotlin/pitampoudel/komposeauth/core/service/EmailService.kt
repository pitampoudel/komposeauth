package pitampoudel.komposeauth.core.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.AppProperties

@Service
class EmailService(private val appProperties: AppProperties) {

    fun javaMailSender(): JavaMailSender {
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