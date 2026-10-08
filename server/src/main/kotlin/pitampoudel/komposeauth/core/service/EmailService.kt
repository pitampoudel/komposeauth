package pitampoudel.komposeauth.core.service

import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import pitampoudel.komposeauth.app_config.service.AppConfigService

@Service
class EmailService(
    private val appConfigService: AppConfigService,
    private val templateEngine: TemplateEngine,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun javaMailSender(): JavaMailSender {
        val config = appConfigService.getConfig()
        val impl = JavaMailSenderImpl()
        impl.host = config.smtpHost
        impl.port = config.smtpPort ?: 587
        impl.username = config.smtpUsername
        impl.password = config.smtpPassword

        val props = impl.javaMailProperties
        // A Properties value can't be null; putting one threw, and so failed every send, whenever no
        // from-address was configured.
        config.smtpFromEmail?.let { props["mail.smtp.from"] = it }
        props["mail.smtp.auth"] = !config.smtpUsername.isNullOrBlank()
        props["mail.smtp.starttls.enable"] = "true"
        // JavaMail waits forever by default, holding the request thread for as long as the server stalls.
        props["mail.smtp.connectiontimeout"] = SMTP_TIMEOUT_MILLIS
        props["mail.smtp.timeout"] = SMTP_TIMEOUT_MILLIS
        props["mail.smtp.writetimeout"] = SMTP_TIMEOUT_MILLIS
        return impl
    }

    private fun render(template: String, baseUrl: String, variables: Map<String, Any?>): String {
        val context = Context().apply {
            // branding defaults
            setVariable("appName", appConfigService.getConfig().name)
            setVariable("logoUrl", appConfigService.getConfig().logoUrl)
            setVariable("brandColor", appConfigService.getConfig().brandColor)
            setVariable("supportEmail", appConfigService.getConfig().supportEmail)
            setVariable("footerText", appConfigService.getConfig().emailFooterText)
            setVariable("baseUrl", baseUrl)
            setVariable("facebookUrl", appConfigService.getConfig().facebookLink)
            setVariable("instagramUrl", appConfigService.getConfig().instagramLink)
            setVariable("youtubeUrl", appConfigService.getConfig().youtubeLink)
            setVariable("linkedinUrl", appConfigService.getConfig().linkedinLink)
            setVariable("tiktokUrl", appConfigService.getConfig().tiktokLink)
            setVariable("privacyUrl", appConfigService.getConfig().privacyLink)
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
            val fromEmail = appConfigService.getConfig().smtpFromEmail
            val fromName = appConfigService.getConfig().smtpFromName
            if (!fromEmail.isNullOrBlank()) {
                helper.setFrom(InternetAddress(fromEmail, fromName))
            }
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(html, true)
            sender.send(message)
            true
        } catch (e: Exception) {
            log.error("Could not send the '{}' email", template, e)
            false
        }
    }

    private companion object {
        const val SMTP_TIMEOUT_MILLIS = "10000"
    }
}