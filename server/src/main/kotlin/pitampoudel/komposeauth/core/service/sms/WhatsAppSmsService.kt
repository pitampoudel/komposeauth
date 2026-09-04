package pitampoudel.komposeauth.core.service.sms

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.service.AppConfigService

class WhatsAppSmsService(
    private val appConfigService: AppConfigService,
    private val restTemplate: RestTemplate
) : SmsService {
    private val logger: Logger = LoggerFactory.getLogger(WhatsAppSmsService::class.java)

    override fun sendSms(phoneNumber: String, message: String) {
        val config = appConfigService.getConfig()

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setBearerAuth(config.whatsappAccessToken.orEmpty())

        val body = mapOf(
            "messaging_product" to "whatsapp",
            "to" to phoneNumber.removePrefix("+"),
            "type" to "text",
            "text" to mapOf("body" to message)
        )
        val entity = HttpEntity(body, headers)

        val response = restTemplate.postForObject(
            "https://graph.facebook.com/v20.0/${config.whatsappPhoneNumberId}/messages",
            entity,
            String::class.java
        )

        logger.debug("WhatsApp message: $message To: $phoneNumber")

        logger.debug("WhatsApp API Response: $response")
    }
}
