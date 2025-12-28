package pitampoudel.komposeauth.organization

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.organization.entity.Organization
import pitampoudel.komposeauth.organization.service.OrganizationService
import pitampoudel.komposeauth.user.repository.UserRepository

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
@AutoConfigureMockMvc
class OrganizationDeleteControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var organizationService: OrganizationService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `delete organization succeeds for member`() = runBlocking {
        val userId = TestAuthHelpers.createUser(mockMvc, json, "org-delete-member@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "org-delete-member@example.com")

        val org = organizationService.save(
            Organization(
                name = "Deletable Org",
                email = "deletable@example.com",
                userIds = listOf(ObjectId(userId)),
                logoUrl = null, country = null, state = null, city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null, description = null, website = null
            )
        )

        mockMvc.delete("/${ApiEndpoints.ORGANIZATIONS}/${org.id}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.message") { value("Organization deleted") }
            }
        }
    }

    @Test
    fun `delete organization fails for non-member`() = runBlocking {
        val memberId = TestAuthHelpers.createUser(mockMvc, json, "org-delete-other-member@example.com")
        val nonMemberCookie = TestAuthHelpers.loginCookie(mockMvc, json, TestAuthHelpers.createUser(mockMvc, json, "org-delete-non-member@example.com"))

        val org = organizationService.save(
            Organization(
                name = "Another Deletable Org",
                email = "another-deletable@example.com",
                userIds = listOf(ObjectId(memberId)),
                logoUrl = null, country = null, state = null, city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null, description = null, website = null
            )
        )

        mockMvc.delete("/${ApiEndpoints.ORGANIZATIONS}/${org.id}") {
            accept = MediaType.APPLICATION_JSON
            cookie(nonMemberCookie)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `delete organization succeeds for admin`() = runBlocking {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, userRepository, "org-delete-admin@example.com")

        val org = organizationService.save(
            Organization(
                name = "Admin-Deletable Org",
                email = "admin-deletable@example.com",
                userIds = emptyList(),
                logoUrl = null, country = null, state = null, city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null, description = null, website = null
            )
        )

        mockMvc.delete("/${ApiEndpoints.ORGANIZATIONS}/${org.id}") {
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.message") { value("Organization deleted") }
            }
        }
    }

    @Test
    fun `delete organization fails for non-existent organization`() = runBlocking {
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, TestAuthHelpers.createUser(mockMvc, json, "org-delete-400-user@example.com"))

        mockMvc.delete("/${ApiEndpoints.ORGANIZATIONS}/${ObjectId()}") {
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
        }.andExpect {
            status { isBadRequest() }
        }
    }
}