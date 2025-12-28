package pitampoudel.komposeauth.kyc

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
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class KycControllerSecurityIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Test
    fun `check pending kyc requires admin - normal user gets 403`() {
        TestAuthHelpers.createUser(mockMvc, json, "normal-kyc@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "normal-kyc@example.com")

        mockMvc.get("/${ApiEndpoints.KYC_PENDING}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `check pending kyc requires auth - unauth gets 401`() {
        mockMvc.get("/${ApiEndpoints.KYC_PENDING}") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `getMine returns 404 when no kyc exists`() {
        TestAuthHelpers.createUser(mockMvc, json, "no-kyc@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "no-kyc@example.com")

        mockMvc.get("/${ApiEndpoints.KYC}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `submit personal info then getMine returns kyc draft`() {
        TestAuthHelpers.createUser(mockMvc, json, "draft-kyc@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "draft-kyc@example.com")

        val body = json.encodeToString(
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
            cookie(cookie)
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.personalInformation.firstName") { value("John") }
            jsonPath("$.status") { value("DRAFT") }
        }

        mockMvc.get("/${ApiEndpoints.KYC}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            jsonPath("$.personalInformation.firstName") { value("John") }
            jsonPath("$.status") { value("DRAFT") }
        }
    }
}
