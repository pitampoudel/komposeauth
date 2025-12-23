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
import org.springframework.test.web.servlet.post
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.TestAuthHelpers
import pitampoudel.komposeauth.core.data.AddressInformation
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.organization.data.CreateOrUpdateOrganizationRequest
import pitampoudel.komposeauth.organization.entity.Organization
import pitampoudel.komposeauth.organization.service.OrganizationService
import pitampoudel.komposeauth.user.repository.UserRepository

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class OrganizationWriteControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var json: Json

    @Autowired
    private lateinit var organizationService: OrganizationService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `create organization succeeds with valid data`() = runBlocking {
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, TestAuthHelpers.createUser(mockMvc, json, "org-write-user@example.com"))

        val request = CreateOrUpdateOrganizationRequest(
            name = "New Org",
            email = "new-org@example.com",
            phoneNumber = "+14155552671",
            countryNameCode = "US",
            address = AddressInformation(null, null, null, null, null),
            description = "A new organization",
            registrationNo = "12345",
            website = "https://new-org.com",
            facebookLink = "",
            logo = null,
            orgId = null
        )

        mockMvc.post("/${ApiEndpoints.ORGANIZATIONS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(CreateOrUpdateOrganizationRequest.serializer(), request)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.message") { value("Organization created successfully") }
            }
        }
    }

    @Test
    fun `update organization succeeds for member`() = runBlocking {
        val userId = TestAuthHelpers.createUser(mockMvc, json, "org-update-member@example.com")
        val cookie = TestAuthHelpers.loginCookie(mockMvc, json, "org-update-member@example.com")

        val org = organizationService.save(
            Organization(
                name = "Original Name",
                email = "original@example.com",
                userIds = listOf(ObjectId(userId)),
                logoUrl = null, country = null, state = null, city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null, description = null, website = null
            )
        )

        val request = CreateOrUpdateOrganizationRequest(
            name = "Updated Name",
            email = "updated@example.com",
            phoneNumber = "+14155552671",
            countryNameCode = "US",
            address = AddressInformation(null, null, null, null, null),
            description = "An updated organization",
            registrationNo = "54321",
            website = "https://updated-org.com",
            facebookLink = "",
            logo = null,
            orgId = org.id.toString()
        )

        mockMvc.post("/${ApiEndpoints.ORGANIZATIONS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(cookie)
            content = json.encodeToString(CreateOrUpdateOrganizationRequest.serializer(), request)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.message") { value("Organization updated successfully") }
            }
        }
    }

    @Test
    fun `update organization fails for non-member`() = runBlocking {
        val memberId = TestAuthHelpers.createUser(mockMvc, json, "org-update-other-member@example.com")
        val nonMemberCookie = TestAuthHelpers.loginCookie(mockMvc, json, TestAuthHelpers.createUser(mockMvc, json, "org-update-non-member@example.com"))

        val org = organizationService.save(
            Organization(
                name = "Another Org",
                email = "another@example.com",
                userIds = listOf(ObjectId(memberId)),
                logoUrl = null, country = null, state = null, city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null, description = null, website = null
            )
        )

        val request = CreateOrUpdateOrganizationRequest(
            name = "Updated Name",
            email = "updated@example.com",
            phoneNumber = "+14155552671",
            countryNameCode = "US",
            address = AddressInformation(null, null, null, null, null),
            description = "An updated organization",
            registrationNo = "54321",
            website = "https://updated-org.com",
            facebookLink = "",
            logo = null,
            orgId = org.id.toString()
        )

        mockMvc.post("/${ApiEndpoints.ORGANIZATIONS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(nonMemberCookie)
            content = json.encodeToString(CreateOrUpdateOrganizationRequest.serializer(), request)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `update organization succeeds for admin`() = runBlocking {
        val (_, adminCookie) = TestAuthHelpers.createAdminAndLogin(mockMvc, json, userRepository, "org-admin@example.com")

        val org = organizationService.save(
            Organization(
                name = "Admin-Updated Org",
                email = "admin-update@example.com",
                userIds = emptyList(),
                logoUrl = null, country = null, state = null, city = null, addressLine1 = null, addressLine2 = null, phoneNumber = null, registrationNo = null, description = null, website = null
            )
        )

        val request = CreateOrUpdateOrganizationRequest(
            name = "Updated by Admin",
            email = "updated-admin@example.com",
            phoneNumber = "+14155552671",
            countryNameCode = "US",
            address = AddressInformation(null, null, null, null, null),
            description = "An organization updated by an admin",
            registrationNo = "admin-reg",
            website = "https://admin-updated.com",
            facebookLink = "",
            logo = null,
            orgId = org.id.toString()
        )

        mockMvc.post("/${ApiEndpoints.ORGANIZATIONS}") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            cookie(adminCookie)
            content = json.encodeToString(CreateOrUpdateOrganizationRequest.serializer(), request)
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.message") { value("Organization updated successfully") }
            }
        }
    }
}