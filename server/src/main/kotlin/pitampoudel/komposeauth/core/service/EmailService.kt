package pitampoudel.komposeauth.core.service

import org.springframework.beans.factory.ObjectProvider
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(private val mailSenderProvider: ObjectProvider<JavaMailSender>) {
    fun sendSimpleMail(
        to: String,
        subject: String? = null,
        text: String? = null
    ): Boolean {
        // Request a fresh JavaMailSender each time to pick up latest SMTP settings
        val javaMailSender = runCatching { mailSenderProvider.getObject() }.getOrNull()
            ?: mailSenderProvider.getIfAvailable() ?: return false
        return try {
            val mailMessage = SimpleMailMessage()
            mailMessage.setTo(to)
            mailMessage.text = text
            mailMessage.subject = subject
            javaMailSender.send(mailMessage)
            true
        } catch (e: Exception) {
            false
        }
    }
}