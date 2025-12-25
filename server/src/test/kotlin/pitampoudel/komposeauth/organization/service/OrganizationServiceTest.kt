package pitampoudel.komposeauth.organization.service

import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import pitampoudel.komposeauth.MongoTestSupport
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
    fun `findByIds returns empty map for empty input`() = runBlocking {
        val result = organizationService.findByIds(emptyList())
        assertTrue(result.isEmpty())
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



}
