package pitampoudel.komposeauth.organization

import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import pitampoudel.komposeauth.MongoContainerTest
import pitampoudel.komposeauth.core.config.canEditOrganization
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.organization.entity.Organization
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrganizationPermissionsTest  : MongoContainerTest() {

    @Test
    fun `canEditOrganization true for admins`() {
        val userId = ObjectId.get()
        val user = User(
            id = userId,
            firstName = "Admin",
            lastName = "User",
            email = "admin@example.com",
            phoneNumber = null,
            roles = listOf("ADMIN"),
        )
        val org = Organization(
            name = "Org",
            email = "org@example.com",
            logoUrl = null,
            country = null,
            state = null,
            city = null,
            addressLine1 = null,
            addressLine2 = null,
            phoneNumber = null,
            registrationNo = null,
            description = null,
            website = null,
            socialLinks = emptyList(),
            userIds = emptyList(),
        )

        assertTrue(canEditOrganization(org, user))
    }

    @Test
    fun `canEditOrganization true for member and false for non-member`() {
        val memberId = ObjectId.get()
        val nonMemberId = ObjectId.get()

        val org = Organization(
            name = "Org",
            email = "org@example.com",
            logoUrl = null,
            country = null,
            state = null,
            city = null,
            addressLine1 = null,
            addressLine2 = null,
            phoneNumber = null,
            registrationNo = null,
            description = null,
            website = null,
            socialLinks = emptyList(),
            userIds = listOf(memberId),
        )

        val member = User(
            id = memberId,
            firstName = "Member",
            lastName = "User",
            email = "m@example.com",
            phoneNumber = null,
        )

        val nonMember = User(
            id = nonMemberId,
            firstName = "Other",
            lastName = "User",
            email = "o@example.com",
            phoneNumber = null,
        )

        assertTrue(canEditOrganization(org, member))
        assertFalse(canEditOrganization(org, nonMember))
    }
}

