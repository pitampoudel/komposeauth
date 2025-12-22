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
        impl.host = appConfigProvider.getConfig().smtpHost
        impl.port = appConfigProvider.getConfig().smtpPort ?: 587
        impl.username = appConfigProvider.getConfig().smtpUsername
        impl.password = appConfigProvider.getConfig().smtpPassword

        val props = impl.javaMailProperties
        props["mail.smtp.from"] = appConfigProvider.getConfig().smtpFromEmail
        props["mail.smtp.auth"] = !appConfigProvider.getConfig().smtpUsername.isNullOrBlank()
        props["mail.smtp.starttls.enable"] = "true"
        return impl
    }

    private fun render(template: String, baseUrl: String, variables: Map<String, Any?>): String {
        val context = Context().apply {
            // branding defaults
            setVariable("appName", appConfigProvider.getConfig().name)
            setVariable("logoUrl", appConfigProvider.getConfig().logoUrl)
            setVariable("brandColor", appConfigProvider.getConfig().brandColor)
            setVariable("supportEmail", appConfigProvider.getConfig().supportEmail)
            setVariable("footerText", appConfigProvider.getConfig().emailFooterText)
            setVariable("baseUrl", baseUrl)
            setVariable("facebookUrl", appConfigProvider.getConfig().facebookLink)
            setVariable("instagramUrl", appConfigProvider.getConfig().instagramLink)
            setVariable("youtubeUrl", appConfigProvider.getConfig().youtubeLink)
            setVariable("linkedinUrl", appConfigProvider.getConfig().linkedinLink)
            setVariable("tiktokUrl", appConfigProvider.getConfig().tiktokLink)
            setVariable("privacyUrl", appConfigProvider.getConfig().privacyLink)
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
            val fromEmail = appConfigProvider.getConfig().smtpFromEmail
            val fromName = appConfigProvider.getConfig().smtpFromName
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