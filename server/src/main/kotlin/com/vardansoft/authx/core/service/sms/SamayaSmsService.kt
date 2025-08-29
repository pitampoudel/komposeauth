package com.vardansoft.authx.core.service.sms

import com.vardansoft.authx.AppProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

class SamayaSmsService(
    private val appProperties: AppProperties,
    private val restTemplate: RestTemplate
) : SmsService {
    val logger: Logger = LoggerFactory.getLogger(SamayaSmsService::class.java)
    override fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

            val formData = buildFormData(phoneNumber, message)
            val entity = HttpEntity(formData, headers)

            val response = restTemplate.postForObject(
                "https://samayasms.com.np/smsapi/index.php",
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
        formData.add("key", appProperties.samayeApiKey)
        formData.add("campaign", "8238")
        formData.add("routeid", "135")
        formData.add("type", "text")
        formData.add("responsetype", "json")
        formData.add("contacts", phoneNumber)
        formData.add("senderid", "FSN_Alert")
        formData.add("msg", message)
        return formData
    }
}