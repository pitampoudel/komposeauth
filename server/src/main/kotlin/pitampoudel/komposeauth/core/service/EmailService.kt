package pitampoudel.komposeauth.core.service

import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import pitampoudel.komposeauth.app_config.service.AppConfigProvider

@Service
class EmailService(
    private val appConfigProvider: AppConfigProvider,
    private val templateEngine: TemplateEngine,
) {

    private fun javaMailSender(): JavaMailSender {
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

    private fun render(template: String, baseUrl: String, variables: Map<String, Any?>): String {
        val context = Context().apply {
            // branding defaults
            setVariable("appName", appConfigProvider.name)
            setVariable("logoUrl", appConfigProvider.logoUrl)
            setVariable("brandColor", appConfigProvider.brandColor)
            setVariable("supportEmail", appConfigProvider.supportEmail)
            setVariable("footerText", appConfigProvider.emailFooterText)
            setVariable("baseUrl", baseUrl)
            setVariable("facebookUrl", appConfigProvider.facebookLink)
            setVariable("instagramUrl", appConfigProvider.instagramLink)
            setVariable("youtubeUrl", appConfigProvider.youtubeLink)
            setVariable("linkedinUrl", appConfigProvider.linkedinLink)
            setVariable("tiktokUrl", appConfigProvider.tiktokLink)
            setVariable("privacyUrl", appConfigProvider.privacyLink)
            variables.forEach { (k, v) -> setVariable(k, v) }
        }
        return templateEngine.process(template, context)
    }

    fun sendHtmlMail(
        baseUrl: String,
        to: String,
        subject: String,
        template: String,
        model: Map<String, Any?> = emptyMap(),
    ): Boolean {
        return try {
            val html = render(
                template = template,
                baseUrl = baseUrl,
                variables = model + mapOf("subject" to subject)
            )
            val sender = javaMailSender()
            val message: MimeMessage = sender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            val fromEmail = appConfigProvider.smtpFromEmail
            val fromName = appConfigProvider.smtpFromName
            if (!fromEmail.isNullOrBlank()) {
                helper.setFrom(InternetAddress(fromEmail, fromName))
            }
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(html, true)
            sender.send(message)
            true
        } catch (e: Exception) {
            false
        }
    }
}