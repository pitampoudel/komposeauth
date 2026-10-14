package pitampoudel.komposeauth.core.config

import com.mongodb.client.model.Filters
import io.flamingock.api.annotations.Change
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Import
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import kotlin.test.assertEquals

/**
 * The server starts with every change applied. A change's own test calls it directly, so only a
 * real start shows that Flamingock found it and could supply what its `@Apply` asks for.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class FlamingockIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Test
    fun `every change has run by the time the server is up`() {
        val scanner = ClassPathScanningCandidateComponentProvider(false).apply {
            addIncludeFilter(AnnotationTypeFilter(Change::class.java))
        }
        val changes = scanner.findCandidateComponents("pitampoudel.komposeauth")
            .map { Class.forName(it.beanClassName).getAnnotation(Change::class.java).id }
            .toSortedSet()

        val applied = mongoTemplate.getCollection("flamingockAuditLog")
            .find(Filters.eq("state", "APPLIED"))
            .map { it.getString("changeId") }
            .toSortedSet()

        assertEquals(changes, applied)
    }
}
