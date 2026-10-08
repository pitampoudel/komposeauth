package pitampoudel.komposeauth.organization.service

import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.organization.repository.OrganizationRepository

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class OrganizationServiceTest {

    @Autowired
    private lateinit var organizationService: OrganizationService

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository






    @Test
    fun `findById returns null for non-existent id`() {
        val result = organizationService.findById(ObjectId.get().toHexString())
        assertNull(result)
    }

    @Test
    fun `findById returns null for invalid id`() {
        val result = organizationService.findById("invalid-id")
        assertNull(result)
    }



}
