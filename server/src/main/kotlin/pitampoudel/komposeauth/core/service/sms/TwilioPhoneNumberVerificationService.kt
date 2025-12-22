package pitampoudel.komposeauth.core.service.sms

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import pitampoudel.komposeauth.app_config.service.AppConfigService
import java.nio.charset.StandardCharsets
import java.util.Base64

class TwilioPhoneNumberVerificationService(
    private val appConfigService: AppConfigService,
    private val restTemplate: RestTemplate
) : PhoneNumberVerificationService {

    private fun basicHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        val auth = "${appConfigService.getConfig().twilioAccountSid}:${appConfigService.getConfig().twilioAuthToken}"
        val encodedAuth =
            Base64.getEncoder().encodeToString(auth.toByteArray(StandardCharsets.UTF_8))
        headers.set(HttpHeaders.AUTHORIZATION, "Basic $encodedAuth")
        return headers
    }

    override fun initiate(phoneNumber: String): Boolean {
        val verifySid = appConfigService.getConfig().twilioVerifyServiceSid
        return try {
            val url = "https://verify.twilio.com/v2/Services/$verifySid/Verifications"
            val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
            formData.add("To", phoneNumber)
            formData.add("Channel", "sms")
            val entity = HttpEntity(formData, basicHeaders())
            restTemplate.postForObject(url, entity, String::class.java)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    override fun verify(phoneNumber: String, code: String): Boolean {
        val verifySid = appConfigService.getConfig().twilioVerifyServiceSid
        return try {
            val url = "https://verify.twilio.com/v2/Services/$verifySid/VerificationCheck"
            val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
            formData.add("To", phoneNumber)
            formData.add("Code", code)
            val entity = HttpEntity(formData, basicHeaders())
            restTemplate.postForObject(url, entity, String::class.java)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}