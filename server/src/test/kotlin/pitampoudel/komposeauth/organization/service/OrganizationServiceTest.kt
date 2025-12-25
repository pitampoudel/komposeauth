package pitampoudel.komposeauth.organization.service

import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.organization.entity.Organization
import pitampoudel.komposeauth.organization.repository.OrganizationRepository

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
class OrganizationServiceTest {

    @Autowired
    private lateinit var organizationService: OrganizationService

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Test
    fun `findByIds returns organizations map by id`() = runBlocking {
        val org1 = organizationRepository.save(
            Organization(
                id = ObjectId.get(),
                name = "Test Org 1",
                userIds = listOf(ObjectId.get())
            )
        )
        val org2 = organizationRepository.save(
            Organization(
                id = ObjectId.get(),
                name = "Test Org 2",
                userIds = listOf(ObjectId.get())
            )
        )

        val result = organizationService.findByIds(listOf(org1.id.toHexString(), org2.id.toHexString()))

        assertEquals(2, result.size)
        assertEquals("Test Org 1", result[org1.id.toHexString()]?.name)
        assertEquals("Test Org 2", result[org2.id.toHexString()]?.name)
    }

    @Test
    fun `findByIds returns empty map for empty input`() = runBlocking {
        val result = organizationService.findByIds(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findByIds ignores invalid ObjectIds`() = runBlocking {
        val org = organizationRepository.save(
            Organization(
                id = ObjectId.get(),
                name = "Valid Org",
                userIds = listOf(ObjectId.get())
            )
        )

        val result = organizationService.findByIds(listOf(org.id.toHexString(), "invalid-id", "also-invalid"))

        assertEquals(1, result.size)
        assertEquals("Valid Org", result[org.id.toHexString()]?.name)
    }

    @Test
    fun `findOrgs returns organizations by ids`() = runBlocking {
        val org1 = organizationRepository.save(
            Organization(
                id = ObjectId.get(),
                name = "Org A",
                userIds = listOf(ObjectId.get())
            )
        )
        val org2 = organizationRepository.save(
            Organization(
                id = ObjectId.get(),
                name = "Org B",
                userIds = listOf(ObjectId.get())
            )
        )

        val result = organizationService.findOrgs(listOf(org1.id.toHexString(), org2.id.toHexString()))

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Org A" })
        assertTrue(result.any { it.name == "Org B" })
    }

    @Test
    fun `findOrgsForUser returns organizations containing user`() = runBlocking {
        val userId = ObjectId.get()
        
        val org1 = organizationRepository.save(
            Organization(
                id = ObjectId.get(),
                name = "User's Org 1",
                userIds = listOf(userId, ObjectId.get())
            )
        )
        val org2 = organizationRepository.save(
            Organization(
                id = ObjectId.get(),
                name = "User's Org 2",
                userIds = listOf(userId)
            )
        )
        
        // Create org without the user
        organizationRepository.save(
            Organization(
                id = ObjectId.get(),
                name = "Other Org",
                userIds = listOf(ObjectId.get())
            )
        )

        val result = organizationService.findOrgsForUser(userId)

        assertEquals(2, result.size)
        assertTrue(result.all { it.userIds.contains(userId) })
    }

    @Test
    fun `findById returns organization when exists`() = runBlocking {
        val org = organizationRepository.save(
            Organization(
                id = ObjectId.get(),
                name = "Find By ID Org",
                userIds = listOf(ObjectId.get())
            )
        )

        val result = organizationService.findById(org.id.toHexString())

        assertNotNull(result)
        assertEquals("Find By ID Org", result?.name)
    }

    @Test
    fun `findById returns null for non-existent id`() = runBlocking {
        val result = organizationService.findById(ObjectId.get().toHexString())
        assertNull(result)
    }

    @Test
    fun `findById returns null for invalid id`() = runBlocking {
        val result = organizationService.findById("invalid-id")
        assertNull(result)
    }

    @Test
    fun `save creates new organization`() = runBlocking {
        val org = Organization(
            id = ObjectId.get(),
            name = "New Org",
            userIds = listOf(ObjectId.get())
        )

        val saved = organizationService.save(org)

        assertEquals("New Org", saved.name)
        assertEquals(org.userIds, saved.userIds)
    }

    @Test
    fun `save updates existing organization`() = runBlocking {
        val org = organizationRepository.save(
            Organization(
                id = ObjectId.get(),
                name = "Original Name",
                userIds = listOf(ObjectId.get())
            )
        )

        val updated = organizationService.save(org.copy(name = "Updated Name"))

        assertEquals("Updated Name", updated.name)
        assertEquals(org.id, updated.id)
    }

    @Test
    fun `delete removes organization`() = runBlocking {
        val org = organizationRepository.save(
            Organization(
                id = ObjectId.get(),
                name = "To Delete",
                userIds = listOf(ObjectId.get())
            )
        )

        organizationService.delete(org.id)

        val result = organizationService.findById(org.id.toHexString())
        assertNull(result)
    }
}
