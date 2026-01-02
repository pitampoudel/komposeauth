package pitampoudel.komposeauth.user.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pitampoudel.core.data.parsePhoneNumber

@Serializable
data class VerifyOtpRequest(
    @SerialName("countryCode")
    val countryCode: String?,
    @SerialName("phoneNumber")
    val phoneNumber: String,
    @SerialName("otp")
    val otp: String
) {
    fun parsedPhoneNumber(): String? {
        return parsePhoneNumber(
            countryNameCode = countryCode,
            phoneNumber = phoneNumber
        )?.fullNumberInE164Format
    }
}