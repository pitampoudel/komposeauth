package pitampoudel.komposeauth.organization

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.organization.entity.Organization
import pitampoudel.komposeauth.organization.service.OrganizationService

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class OrganizationReadControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var organizationService: OrganizationService

    @Test
    fun `getOrganizations returns only organizations for the current user`() = runBlocking {
        val userId = TestAuthHelpers.createUser(mockMvc, json, "org-read-test-user@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "org-read-test-user@example.com")

        val org = organizationService.save(
            Organization(
                name = "Test Org",
                email = "org@example.com",
                userIds = listOf(ObjectId(userId)),
                logoUrl = null, country = null, state = null, city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null, description = null, website = null
            )
        )

        mockMvc.get("/${ApiEndpoints.ORGANIZATIONS}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$[0].id") { value(org.id.toString()) }
                jsonPath("$[0].name") { value("Test Org") }
            }
        }
    }

    @Test
    fun `getOrganizations with ids returns the correct subset`() = runBlocking {
        val userId = TestAuthHelpers.createUser(mockMvc, json, "org-subset-user@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "org-subset-user@example.com")

        val org1 = organizationService.save(
            Organization(name = "Org 1", email = "org1@example.com", userIds = listOf(ObjectId(userId)), logoUrl = null, country = null, state = null, city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null, description = null, website = null)
        )
        val org2 = organizationService.save(
            Organization(name = "Org 2", email = "org2@example.com", userIds = listOf(ObjectId(userId)), logoUrl = null, country = null, state = null, city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null, description = null, website = null)
        )
        organizationService.save(
            Organization(name = "Org 3", email = "org3@example.com", userIds = listOf(ObjectId(userId)), logoUrl = null, country = null, state = null, city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null, description = null, website = null)
        )

        mockMvc.get("/${ApiEndpoints.ORGANIZATIONS}?ids=${org1.id},${org2.id}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.length()") { value(2) }
                jsonPath("$[?(@.id == '${org1.id}')]") { exists() }
                jsonPath("$[?(@.id == '${org2.id}')]") { exists() }
            }
        }
    }

    @Test
    fun `getOrganizationById returns organization when user is a member`() = runBlocking {
        val userId = TestAuthHelpers.createUser(mockMvc, json, "org-by-id-user@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "org-by-id-user@example.com")

        val org = organizationService.save(
            Organization(
                name = "Test Org By Id",
                email = "org-by-id@example.com",
                userIds = listOf(ObjectId(userId)),
                logoUrl = null, country = null, state = null, city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null, description = null, website = null
            )
        )

        mockMvc.get("/${ApiEndpoints.ORGANIZATIONS}/${org.id}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.id") { value(org.id.toString()) }
                jsonPath("$.name") { value("Test Org By Id") }
            }
        }
    }

    @Test
    fun `getOrganizationById returns 403 for user who is not a member`() = runBlocking {
        val memberId = TestAuthHelpers.createUser(mockMvc, json, "org-member@example.com")
        val nonMemberCookie = TestAuthHelpers.loginCookie(mockMvc, json, "org-non-member@example.com")

        val org = organizationService.save(
            Organization(
                name = "Restricted Org",
                email = "restricted@example.com",
                userIds = listOf(ObjectId(memberId)),
                logoUrl = null, country = null, state = null, city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null, description = null, website = null
            )
        )

        mockMvc.get("/${ApiEndpoints.ORGANIZATIONS}/${org.id}") {
            accept = MediaType.APPLICATION_JSON
            cookie(nonMemberCookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `getOrganizationById returns 400 for non-existent organization`() = runBlocking {
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, TestAuthHelpers.createUser(mockMvc, json, "org-400-user@example.com"))

        mockMvc.get("/${ApiEndpoints.ORGANIZATIONS}/${ObjectId()}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isBadRequest() }
        }
    }
}