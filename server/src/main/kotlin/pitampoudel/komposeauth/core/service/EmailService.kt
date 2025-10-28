package pitampoudel.komposeauth.core.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(val javaMailSender: JavaMailSender) {

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
            javaMailSender.send(mailMessage)
            true
        } catch (e: Exception) {
            false
        }
    }
}