package pitampoudel.komposeauth.kyc

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.entity.KycVerification
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThirdFactorWarningsTest {

    private fun kyc(
        thirdFactorVerified: Boolean? = true,
        thirdFactorFaceMatch: Double? = 95.0,
        thirdFactorBypassed: Boolean? = false,
        thirdFactorForcedNext: Boolean? = false
    ) = KycVerification(
        userId = ObjectId.get(),
        country = "NP",
        nationality = "nepali",
        firstName = "A",
        middleName = null,
        lastName = "B",
        dateOfBirth = LocalDate.parse("2000-01-01"),
        gender = KycResponse.Gender.MALE,
        fatherName = null,
        grandFatherName = null,
        maritalStatus = null,
        thirdFactorVerified = thirdFactorVerified,
        thirdFactorFaceMatch = thirdFactorFaceMatch,
        thirdFactorBypassed = thirdFactorBypassed,
        thirdFactorForcedNext = thirdFactorForcedNext
    )

    @Test
    fun `a clean session warns about nothing`() {
        assertEquals(emptyList(), kyc().thirdFactorWarnings)
    }

    @Test
    fun `a manual submission carrying no third factor signals warns about nothing`() {
        val manual = kyc(
            thirdFactorVerified = null,
            thirdFactorFaceMatch = null,
            thirdFactorBypassed = null,
            thirdFactorForcedNext = null
        )
        assertEquals(emptyList(), manual.thirdFactorWarnings)
    }

    @Test
    fun `flags a failed verdict, a bypass, a force-advance and a weak face match`() {
        val warnings = kyc(
            thirdFactorVerified = false,
            thirdFactorFaceMatch = 64.7401,
            thirdFactorBypassed = true,
            thirdFactorForcedNext = true
        ).thirdFactorWarnings

        assertEquals(4, warnings.size)
        assertTrue(warnings.any { it.contains("not verified") })
        assertTrue(warnings.any { it.contains("bypassed") })
        assertTrue(warnings.any { it.contains("force-advanced") })
        assertTrue(warnings.any { it.contains("64.7%") })
    }

    @Test
    fun `a face match on the threshold is not flagged`() {
        assertEquals(emptyList(), kyc(thirdFactorFaceMatch = 70.0).thirdFactorWarnings)
        assertEquals(1, kyc(thirdFactorFaceMatch = 69.9).thirdFactorWarnings.size)
    }
}
