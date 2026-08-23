package pitampoudel.komposeauth.core.service

import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import pitampoudel.komposeauth.app_config.service.AppConfigService
import java.time.Duration

@Service
class EmailService(
    private val appConfigService: AppConfigService,
    private val templateEngine: TemplateEngine,
    /**
     * How long a send may spend connecting, waiting or writing before it gives up. See where it is
     * applied below for why leaving it to JavaMail is not an option, and `app.mail.timeout` in
     * `application.yml` for the deployment knob.
     */
    @Value("\${app.mail.timeout:15s}") private val mailTimeout: Duration,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** The mail host, for the log. The recipient and the body are deliberately not logged. */
    private fun host(): String = appConfigService.getConfig().smtpHost ?: "no configured host"

    private fun javaMailSender(): JavaMailSender {
        val impl = JavaMailSenderImpl()
        impl.host = appConfigService.getConfig().smtpHost
        impl.port = appConfigService.getConfig().smtpPort ?: 587
        impl.username = appConfigService.getConfig().smtpUsername
        impl.password = appConfigService.getConfig().smtpPassword

        val props = impl.javaMailProperties
        props["mail.smtp.from"] = appConfigService.getConfig().smtpFromEmail
        props["mail.smtp.auth"] = !appConfigService.getConfig().smtpUsername.isNullOrBlank()
        props["mail.smtp.starttls.enable"] = "true"
        // Without these three, a send blocks forever. JavaMail's default for all of them is 0,
        // which means "wait indefinitely", and a mail host that drops packets rather than refusing
        // them -- an SMTP port filtered by the platform, a provider rate-limiting by going quiet --
        // never gives the socket anything to react to. The catch below never fires, because nothing
        // is ever thrown; the request thread is simply pinned, and it is pinned for good rather
        // than for the length of the request, so each one costs the server a thread permanently.
        // The visitor sees a page that spins until a proxy somewhere gives up on it.
        val timeoutMillis = mailTimeout.toMillis().coerceAtLeast(1).toString()
        props["mail.smtp.connectiontimeout"] = timeoutMillis
        props["mail.smtp.timeout"] = timeoutMillis
        props["mail.smtp.writetimeout"] = timeoutMillis
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
            // Swallowed silently until now, which left a failing mail host looking like nothing at
            // all: callers only see `false`, and the one that logs it says which user it was for
            // and nothing about why.
            log.error("Could not send '{}' mail to a recipient via {}", template, host(), e)
            false
        }
    }
}
