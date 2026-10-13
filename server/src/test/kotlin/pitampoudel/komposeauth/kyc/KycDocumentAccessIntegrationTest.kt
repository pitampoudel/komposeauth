package pitampoudel.komposeauth.kyc

import jakarta.servlet.http.Cookie
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.entity.KycVerification
import pitampoudel.komposeauth.kyc.repository.KycVerificationRepository
import pitampoudel.komposeauth.kyc.service.KycDocumentSealing
import pitampoudel.komposeauth.user.repository.UserRepository
import java.util.Base64
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A KYC document is stored where nobody can read it by its address, and is read back through this
 * server by the person it belongs to and by an admin, and by nobody else.
 */
@SpringBootTest(properties = ["spring.main.allow-bean-definition-overriding=true"])
@ActiveProfiles("test")
@Import(TestConfig::class, KycFlowTestOverrides::class)
@AutoConfigureMockMvc
class KycDocumentAccessIntegrationTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var json: Json
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var kycRepository: KycVerificationRepository
    @Autowired private lateinit var storage: KycFlowTestOverrides.InMemoryStorage
    @Autowired private lateinit var sealing: KycDocumentSealing

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    private val front = jpeg + "front of a citizenship card".toByteArray()
    private val back = jpeg + "back of a citizenship card".toByteArray()
    private val selfie = jpeg + "a face".toByteArray()

    private fun ByteArray.mentions(text: String) = String(this, Charsets.ISO_8859_1).contains(text)

    private fun submitDocuments(cookie: Cookie, documentNumber: String) {
        mockMvc.post("/${ApiEndpoints.KYC_PERSONAL_INFO}") {
            contentType = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(
                buildJsonObject {
                    put("country", JsonPrimitive("NP"))
                    put("nationality", JsonPrimitive("NP"))
                    put("firstName", JsonPrimitive("Sita"))
                    put("middleName", JsonNull)
                    put("lastName", JsonPrimitive("Sharma"))
                    put("dateOfBirth", JsonPrimitive(LocalDate.parse("1995-01-01").toString()))
                    put("gender", JsonPrimitive("FEMALE"))
                    put("fatherName", JsonNull)
                    put("grandFatherName", JsonNull)
                    put("maritalStatus", JsonNull)
                }
            )
        }.andExpect { status { isOk() } }

        fun encoded(bytes: ByteArray) = buildJsonObject {
            put("base64EncodedData", JsonPrimitive(Base64.getEncoder().encodeToString(bytes)))
            put("mimeType", JsonPrimitive("image/jpeg"))
            put("name", JsonPrimitive("scan.jpg"))
        }
        val result = mockMvc.post("/${ApiEndpoints.KYC_DOCUMENTS}") {
            contentType = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(
                buildJsonObject {
                    put("documentType", JsonPrimitive("CITIZENSHIP"))
                    put("documentNumber", JsonPrimitive(documentNumber))
                    put("documentIssuedDate", JsonPrimitive(LocalDate.parse("2012-01-01").toString()))
                    put("documentExpiryDate", JsonNull)
                    put("documentIssuedPlace", JsonPrimitive("Kathmandu"))
                    put("documentFront", encoded(front))
                    put("documentBack", encoded(back))
                    put("selfie", encoded(selfie))
                }
            )
        }.andReturn()
        mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk)
    }

    private fun documentInformation(cookie: Cookie): JsonObject {
        val body = mockMvc.get("/${ApiEndpoints.KYC}") { cookie(cookie) }
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString
        return json.parseToJsonElement(body).jsonObject.getValue("documentInformation").jsonObject
    }

    @Test
    fun `a submitted document is stored sealed and read back only by its owner and an admin`() {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, userRepository, "kyc-docs-admin@example.com")
        val ownerId = TestAuthHelpers.createUser(mockMvc, json, "kyc-docs-owner@example.com")
        val ownerCookie = TestAuthHelpers.loginCookie(mockMvc, json, "kyc-docs-owner@example.com")
        TestAuthHelpers.createUser(mockMvc, json, "kyc-docs-other@example.com")
        val otherCookie = TestAuthHelpers.loginCookie(mockMvc, json, "kyc-docs-other@example.com")

        submitDocuments(ownerCookie, "CIT-${ownerId.takeLast(8)}")

        // The bucket holds no readable copy, so its address is worth nothing to whoever has it.
        val stored = assertNotNull(storage.files["test://kyc/$ownerId/front"])
        assertFalse(stored.contentEquals(front))
        assertFalse(stored.mentions("citizenship"))

        // The address handed out is this server's, never the bucket's.
        val info = documentInformation(ownerCookie)
        val frontUrl = info.getValue("documentFrontUrl").jsonPrimitive.content
        assertEquals("http://localhost/${ApiEndpoints.KYC}/$ownerId/documents/front", frontUrl)
        assertEquals("http://localhost/${ApiEndpoints.KYC}/$ownerId/documents/selfie", info.getValue("selfieUrl").jsonPrimitive.content)
        val frontPath = frontUrl.removePrefix("http://localhost")

        mockMvc.get(frontPath).andExpect { status { isUnauthorized() } }

        for (reader in listOf(ownerCookie, adminCookie)) {
            val response = mockMvc.get(frontPath) { cookie(reader) }
                .andExpect {
                    status { isOk() }
                    content { contentType(MediaType.IMAGE_JPEG) }
                }.andReturn().response
            assertContentEquals(front, response.contentAsByteArray)
            assertTrue(response.getHeader("Cache-Control").orEmpty().contains("no-store"))
        }
        assertContentEquals(
            back,
            mockMvc.get("/${ApiEndpoints.KYC}/$ownerId/documents/back") { cookie(adminCookie) }
                .andReturn().response.contentAsByteArray
        )

        // Somebody else's file answers as if it were not there.
        mockMvc.get(frontPath) { cookie(otherCookie) }.andExpect { status { isNotFound() } }
        mockMvc.get("/${ApiEndpoints.KYC}/$ownerId/documents/passport") { cookie(ownerCookie) }
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `a file stored before sealing is sealed where it is and still reads back`() {
        val ownerId = TestAuthHelpers.createUser(mockMvc, json, "kyc-legacy-owner@example.com")
        val ownerCookie = TestAuthHelpers.loginCookie(mockMvc, json, "kyc-legacy-owner@example.com")
        val frontUrl = storage.upload("kyc/$ownerId/front", "image/jpeg", front)
        kycRepository.save(
            KycVerification(
                userId = ObjectId(ownerId),
                country = "NP",
                nationality = "NP",
                firstName = "Ram",
                middleName = null,
                lastName = "Thapa",
                dateOfBirth = java.time.LocalDate.parse("1990-01-01"),
                gender = KycResponse.Gender.MALE,
                fatherName = null,
                grandFatherName = null,
                maritalStatus = null,
                documentFrontUrl = frontUrl,
                selfieUrl = "test://kyc/$ownerId/selfie-that-was-deleted",
                status = KycResponse.Status.PENDING
            )
        )
        val frontPath = "/${ApiEndpoints.KYC}/$ownerId/documents/front"

        // Until it is sealed, an old file is still served as it is.
        assertContentEquals(front, mockMvc.get(frontPath) { cookie(ownerCookie) }.andReturn().response.contentAsByteArray)

        val outcome = sealing.sealStoredFiles()
        assertTrue(outcome.failed.isEmpty(), "$outcome")
        assertTrue("$ownerId/selfie" in outcome.missing, "$outcome")

        val stored = assertNotNull(storage.files[kycRepository.findByUserId(ObjectId(ownerId))?.documentFrontUrl])
        assertFalse(stored.mentions("citizenship"))
        assertContentEquals(front, mockMvc.get(frontPath) { cookie(ownerCookie) }.andReturn().response.contentAsByteArray)

        // Running it again finds nothing left to seal.
        val again = sealing.sealStoredFiles()
        assertEquals(0, again.sealed)
        assertTrue(again.failed.isEmpty(), "$again")
    }
}
