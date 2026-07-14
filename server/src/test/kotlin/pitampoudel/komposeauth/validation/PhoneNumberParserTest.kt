package pitampoudel.komposeauth.validation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import pitampoudel.core.data.parsePhoneNumber

class PhoneNumberParserTest {

    @Test
    fun `accepts real Nepal number with prefix missing from bundled metadata`() {
        val parsed = parsePhoneNumber(countryNameCode = null, phoneNumber = "+9779714345404")
        assertNotNull(parsed)
        assertEquals("+9779714345404", parsed.fullNumberInE164Format)
        assertEquals("NP", parsed.countryNameCode)
    }

    @Test
    fun `accepts real Nepal number given as local form with country code`() {
        val parsed = parsePhoneNumber(countryNameCode = "NP", phoneNumber = "9714345404")
        assertNotNull(parsed)
        assertEquals("+9779714345404", parsed.fullNumberInE164Format)
    }

    @Test
    fun `still accepts a fully valid Nepal number`() {
        val parsed = parsePhoneNumber(countryNameCode = null, phoneNumber = "+9779812345678")
        assertNotNull(parsed)
        assertEquals("+9779812345678", parsed.fullNumberInE164Format)
    }

    @Test
    fun `rejects too-short number`() {
        assertNull(parsePhoneNumber(countryNameCode = "NP", phoneNumber = "12345"))
    }

    @Test
    fun `rejects too-long number`() {
        assertNull(parsePhoneNumber(countryNameCode = "NP", phoneNumber = "97143454049999"))
    }

    @Test
    fun `rejects non-numeric garbage`() {
        assertNull(parsePhoneNumber(countryNameCode = "NP", phoneNumber = "not-a-phone"))
    }
}
