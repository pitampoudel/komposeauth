package pitampoudel.komposeauth.kyc

import jakarta.servlet.http.Cookie
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertTrue

@SpringBootTest(properties = ["spring.main.allow-bean-definition-overriding=true"])
@ActiveProfiles("test")
@Import(TestConfig::class, KycFlowTestOverrides::class)
@AutoConfigureMockMvc
class KycFlowIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var userRepository: UserRepository

    private fun createAndLogin(email: String): Cookie {
        TestAuthHelpers.createUser(mockMvc, json, email)
        return TestAuthHelpers.loginCookie(mockMvc, json, email)
    }

    private fun grantAdmin(adminCookie: Cookie, userId: String) {
        mockMvc.post("/admins/$userId") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `full kyc flow - user submits docs then admin approves and it disappears from pending`() {
        val (adminId, adminCookie) = TestAuthHelpers.createAdminAndLogin(
            mockMvc = mockMvc,
            json = json,
            userRepository = userRepository,
            email = "admin-kyc-flow@example.com",
        )

        val userId = TestAuthHelpers.createUser(mockMvc, json, "user-kyc-flow@example.com")
        val userCookie = TestAuthHelpers.loginCookie(mockMvc, json, "user-kyc-flow@example.com")

        // Step 1: user submits personal information.
        val personalInfoBody = json.encodeToString(
            buildJsonObject {
                put("country", JsonPrimitive("NP"))
                put("nationality", JsonPrimitive("NP"))
                put("firstName", JsonPrimitive("John"))
                put("middleName", JsonPrimitive("M"))
                put("lastName", JsonPrimitive("Doe"))
                put("dateOfBirth", JsonPrimitive(LocalDate.parse("2000-01-01").toString()))
                put("gender", JsonPrimitive("MALE"))
                put("fatherName", JsonPrimitive("Dad"))
                put("grandFatherName", JsonPrimitive("Grand"))
                put("maritalStatus", JsonPrimitive("UNMARRIED"))
            }
        )

        mockMvc.post("/${ApiEndpoints.KYC_PERSONAL_INFO}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(userCookie)
            content = personalInfoBody
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("DRAFT") }
        }

        // Step 2: user submits documents -> should go to PENDING.
        // EncodedData expects keys: base64EncodedData, mimeType, name
        val encodedDoc = buildJsonObject {
            put("base64EncodedData", JsonPrimitive("aGVsbG8="))
            put("mimeType", JsonPrimitive("image/png"))
            put("name", JsonPrimitive("doc.png"))
        }

        val documentsBody = json.encodeToString(
            buildJsonObject {
                put("documentType", JsonPrimitive("NATIONAL_ID"))
                put("documentNumber", JsonPrimitive("DOC-${userId.take(6)}"))
                put("documentIssuedDate", JsonPrimitive(LocalDate.parse("2010-01-01").toString()))
                put("documentExpiryDate", JsonPrimitive(null as String?))
                put("documentIssuedPlace", JsonPrimitive("Kathmandu"))
                put("documentFront", encodedDoc)
                put("documentBack", encodedDoc)
                put("selfie", encodedDoc)
            }
        )

        val mvcResult = mockMvc.post("/${ApiEndpoints.KYC_DOCUMENTS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(userCookie)
            content = documentsBody
        }.andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PENDING"))

        // Step 3: admin sees it in pending list.
        val pendingBodyBefore = mockMvc.get("/${ApiEndpoints.KYC_PENDING}") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsString

        assertTrue(
            pendingBodyBefore.contains(userId),
            "Expected pending list to contain userId=$userId but got: $pendingBodyBefore"
        )

        // Step 4: admin approves.
        mockMvc.post("/kyc/$userId/approve") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("APPROVED") }
        }

        // Step 5: pending list should no longer include user.
        val pendingBodyAfter = mockMvc.get("/${ApiEndpoints.KYC_PENDING}") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsString

        assertTrue(
            !pendingBodyAfter.contains(userId),
            "Expected pending list NOT to contain userId=$userId but got: $pendingBodyAfter"
        )

        // Safety check: admin can still access (remains admin)
        mockMvc.get("/admins") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
        }
    }
}
