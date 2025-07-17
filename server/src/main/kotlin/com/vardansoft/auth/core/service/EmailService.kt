package com.vardansoft.auth.core.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(val javaMailSender: JavaMailSender) {

    fun sendSimpleMail(
        to: String,
        subject: String? = null,
        text: String? = null
    ) {
        try {
            val mailMessage = SimpleMailMessage()
            mailMessage.setTo(to)
            mailMessage.text = text
            mailMessage.subject = subject
            javaMailSender.send(mailMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}