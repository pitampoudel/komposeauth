package pitampoudel.core.data

import io.michaelrocks.libphonenumber.kotlin.PhoneNumberUtil
import io.michaelrocks.libphonenumber.kotlin.metadata.defaultMetadataLoader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhoneNumber(
    @SerialName("nationalNumber")
    val nationalNumber: Long,
    @SerialName("countryNameCode")
    val countryNameCode: String?,
    @SerialName("fullNumberInE164Format")
    val fullNumberInE164Format: String
)

fun parsePhoneNumber(countryNameCode: String?, phoneNumber: String): PhoneNumber? {
    val phoneUtil = PhoneNumberUtil.createInstance(metadataLoader = defaultMetadataLoader())
    return try {
        val num = phoneUtil.parse(phoneNumber, countryNameCode)
        // Accept numbers that are valid OR merely "possible" (correct country code and length).
        // libphonenumber's bundled per-carrier prefix metadata lags real-world allocations
        // (e.g. newly issued Nepal mobile prefixes), so isValidNumber wrongly rejects live numbers.
        // isPossibleNumber still rejects malformed input (wrong length / unknown country code).
        if (phoneUtil.isValidNumber(num) || phoneUtil.isPossibleNumber(num)) PhoneNumber(
            nationalNumber = num.nationalNumber,
            countryNameCode = phoneUtil.getRegionCodeForCountryCode(num.countryCode),
            fullNumberInE164Format = phoneUtil.format(
                num, PhoneNumberUtil.PhoneNumberFormat.E164
            )
        )
        else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}