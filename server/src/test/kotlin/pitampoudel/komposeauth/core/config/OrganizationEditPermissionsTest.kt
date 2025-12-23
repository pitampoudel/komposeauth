package pitampoudel.komposeauth.core.config

import org.bson.types.ObjectId
import pitampoudel.komposeauth.organization.entity.Organization
import pitampoudel.komposeauth.user.entity.User
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrganizationEditPermissionsTest {

    private fun user(id: ObjectId = ObjectId.get(), roles: List<String> = listOf("USER")): User = User(
        id = id,
        firstName = "Test",
        lastName = "User",
        email = "u@example.com",
        phoneNumber = null,
        roles = roles,
        emailVerified = false,
        phoneNumberVerified = false,
        picture = null,
        socialLinks = emptyList(),
        passwordHash = null,
        deactivated = false,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun `admin can edit any organization`() {
        val admin = user(roles = listOf("ADMIN"))
        val org = Organization(
            name = "Acme",
            email = "acme@example.com",
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
            userIds = emptyList()
        )

        assertTrue(canEditOrganization(org, admin))
    }

    @Test
    fun `member can edit when their id is present`() {
        val memberId = ObjectId.get()
        val member = user(memberId)
        val org = Organization(
            name = "Acme",
            email = "acme@example.com",
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
            userIds = listOf(memberId)
        )

        assertTrue(canEditOrganization(org, member))
    }

    @Test
    fun `non member cannot edit`() {
        val memberId = ObjectId.get()
        val member = user(ObjectId.get())
        val org = Organization(
            name = "Acme",
            email = "acme@example.com",
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
            userIds = listOf(memberId)
        )

        assertFalse(canEditOrganization(org, member))
    }
}

