package pitampoudel.komposeauth.kyc

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import pitampoudel.komposeauth.core.config.SerializationConfig
import pitampoudel.komposeauth.core.data.AddressInformation
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.data.PersonalInformation
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Uses the application's own [Json] bean rather than a copy, so a change to its configuration is
 * caught here instead of on the wire.
 */
class KycResponseSerializationTest {

    private val json: Json = SerializationConfig().kotlinxJson()

    private fun address() = AddressInformation(
        country = "NP",
        state = "Bagmati",
        city = "Kathmandu",
        addressLine1 = "Line 1"
    )

    private fun kyc(warnings: List<String>) = KycResponse(
        userId = "68b0f0f0f0f0f0f0f0f0f0f0",
        personalInformation = PersonalInformation(
            country = "NP",
            nationality = "nepali",
            firstName = "Test",
            middleName = null,
            lastName = "User",
            dateOfBirth = LocalDate.parse("2000-01-01"),
            gender = KycResponse.Gender.MALE,
            fatherName = null,
            grandFatherName = null,
            maritalStatus = null
        ),
        currentAddress = address(),
        permanentAddress = address(),
        documentInformation = KycResponse.DocumentInformationResponse(
            documentType = null,
            documentNumber = null,
            documentIssuedDate = null,
            documentExpiryDate = null,
            documentIssuedPlace = null,
            documentFrontUrl = null,
            documentBackUrl = null,
            selfieUrl = null
        ),
        status = KycResponse.Status.PENDING,
        thirdFactorWarnings = warnings
    )

    /**
     * `encodeDefaults` is off, so an empty list — the property's default — would otherwise be
     * dropped and every clean submission would arrive without the field at all.
     */
    @Test
    fun `thirdFactorWarnings is present even when nothing stood out`() {
        val encoded = json.encodeToString(kyc(emptyList()))
        val warnings = json.parseToJsonElement(encoded).jsonObject["thirdFactorWarnings"]

        assertTrue(warnings != null, "thirdFactorWarnings must be encoded even when empty, got: $encoded")
        assertTrue(warnings.jsonArray.isEmpty())
    }

    @Test
    fun `thirdFactorWarnings round-trips the warnings a submission raised`() {
        val warnings = listOf("Third factor not verified", "Face match 64.7%")
        val encoded = json.encodeToString(kyc(warnings))

        assertEquals(warnings, json.decodeFromString<KycResponse>(encoded).thirdFactorWarnings)
    }
}
