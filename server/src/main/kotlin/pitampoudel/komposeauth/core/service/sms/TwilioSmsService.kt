package pitampoudel.komposeauth.core.service.sms

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.config.service.AppConfigProvider
import java.nio.charset.StandardCharsets
import java.util.Base64

class TwilioSmsService(
    private val appConfigProvider: AppConfigProvider,
    private val restTemplate: RestTemplate
) : SmsService {
    private val logger: Logger = LoggerFactory.getLogger(TwilioSmsService::class.java)

    override fun sendSms(phoneNumber: String, message: String): Boolean {
        val accountSid = appConfigProvider.twilioAccountSid
        val authToken = appConfigProvider.twilioAuthToken
        val fromNumber = appConfigProvider.twilioFromNumber

        if (accountSid.isNullOrBlank() || authToken.isNullOrBlank() || fromNumber.isNullOrBlank()) {
            logger.debug("Twilio configuration missing; cannot send SMS.")
            return false
        }

        return try {
            val url = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"

            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
            // Basic Auth header
            val auth = "$accountSid:$authToken"
            val encodedAuth =
                Base64.getEncoder().encodeToString(auth.toByteArray(StandardCharsets.UTF_8))
            headers.set(HttpHeaders.AUTHORIZATION, "Basic $encodedAuth")

            val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
            formData.add("To", phoneNumber)
            formData.add("From", fromNumber)
            formData.add("Body", message)

            val entity = HttpEntity(formData, headers)
            val response = restTemplate.postForObject(url, entity, String::class.java)

            logger.debug("SMS (Twilio): $message To: $phoneNumber")
            logger.debug("Twilio API Response: $response")
            true
        } catch (e: Exception) {
            logger.debug("Twilio SMS sending failed: ${e.message}")
            false
        }
    }
}