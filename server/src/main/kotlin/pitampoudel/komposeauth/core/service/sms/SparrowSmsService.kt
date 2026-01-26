package pitampoudel.komposeauth.core.service.sms

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.service.AppConfigService

class SparrowSmsService(
    private val appConfigService: AppConfigService,
    private val restTemplate: RestTemplate
) : SmsService {
    private val logger: Logger = LoggerFactory.getLogger(SparrowSmsService::class.java)
    override fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

            val formData = buildFormData(phoneNumber, message)
            val entity = HttpEntity(formData, headers)

            val response = restTemplate.postForObject(
                "https://api.sparrowsms.com/v2/sms",
                entity,
                String::class.java
            )

            logger.debug("SMS: $message To: $phoneNumber")

            logger.debug("SMS API Response: $response")
            true
        } catch (e: Exception) {
            logger.debug("SMS sending failed: ${e.message}")
            false
        }
    }

    private fun buildFormData(phoneNumber: String, message: String): MultiValueMap<String, String> {
        val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
        formData.add("token", appConfigService.getConfig().sparrowApiToken)
        formData.add("from", appConfigService.getConfig().sparrowFromNumber)
        formData.add("to", phoneNumber)
        formData.add("text", message)
        return formData
    }
}
