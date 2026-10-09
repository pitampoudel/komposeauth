package pitampoudel.komposeauth.otp.service

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForObject
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.app_config.service.AppConfigService

class TwilioPhoneNumberVerificationService(
    private val appConfigService: AppConfigService,
    private val restTemplate: RestTemplate
) : PhoneNumberVerificationService {

    private fun basicHeaders(): HttpHeaders = HttpHeaders().apply {
        val config = appConfigService.getConfig()
        contentType = MediaType.APPLICATION_FORM_URLENCODED
        setBasicAuth(config.twilioAccountSid.orEmpty(), config.twilioAuthToken.orEmpty())
    }

    override fun initiate(phoneNumber: String): MessageResponse {
        val verifySid = appConfigService.getConfig().twilioVerifyServiceSid
        val url = "https://verify.twilio.com/v2/Services/$verifySid/Verifications"
        val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
        formData.add("To", phoneNumber)
        formData.add("Channel", "sms")
        val entity = HttpEntity(formData, basicHeaders())
        restTemplate.postForObject<String>(url, entity)
        return MessageResponse("An OTP has just been sent to $phoneNumber")
    }


    /**
     * Twilio answers a wrong code with a 200 whose status stays `pending`, so a successful call proves
     * nothing; only `approved` is a match. A verification it no longer has (expired, already used, or
     * out of attempts) is a 404.
     */
    override fun verify(phoneNumber: String, code: String): Boolean {
        val verifySid = appConfigService.getConfig().twilioVerifyServiceSid
        val url = "https://verify.twilio.com/v2/Services/$verifySid/VerificationCheck"
        val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
        formData.add("To", phoneNumber)
        formData.add("Code", code)
        val response = try {
            restTemplate.postForObject<String>(url, HttpEntity(formData, basicHeaders()))
        } catch (e: HttpClientErrorException.NotFound) {
            return false
        }
        val status = response?.let { Json.parseToJsonElement(it).jsonObject["status"]?.jsonPrimitive?.contentOrNull }
        return status == "approved"
    }
}
