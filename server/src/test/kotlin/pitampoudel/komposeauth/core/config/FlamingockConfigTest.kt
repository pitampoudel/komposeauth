package pitampoudel.komposeauth.core.config

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.EnableFlamingock
import io.flamingock.api.annotations.Recovery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.data.mongodb.core.MongoTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Builds Flamingock's own target system and audit store, with only the MongoDB client mocked, so a
 * Flamingock upgrade that changes what this config calls fails here rather than at a deployment's
 * start.
 */
class FlamingockConfigTest {

    @Test
    fun `builds a target system and audit store without a database`() {
        val database = mockk<MongoDatabase>(relaxed = true)
        every { database.name } returns "komposeauth-test"
        val mongoTemplate = mockk<MongoTemplate>(relaxed = true)
        every { mongoTemplate.db } returns database

        val config = FlamingockConfig()
        val targetSystem = config.mongoDBSyncTargetSystem(mockk<MongoClient>(relaxed = true), mongoTemplate)
        assertEquals(FlamingockConfig.TARGET_SYSTEM, targetSystem.id)
        assertEquals("komposeauth-test", targetSystem.databaseName)
        assertNotNull(config.communityAuditStore(targetSystem))
    }
}

/**
 * Every package holding a change is a declared stage. A change in a package nobody declared
 * compiles, the server starts, and its data is simply never migrated, so this is the one failure of
 * the arrangement nothing else would show.
 */
class FlamingockStagesTest {

    private val changes: List<String> = ClassPathScanningCandidateComponentProvider(false)
        .apply { addIncludeFilter(AnnotationTypeFilter(Change::class.java)) }
        .findCandidateComponents("pitampoudel.komposeauth")
        .mapNotNull { it.beanClassName }

    @Test
    fun `every package holding a change is a declared stage`() {
        val declared = FlamingockConfig::class.java.getAnnotation(EnableFlamingock::class.java)
            .stages.map { it.location }.toSet()
        assertTrue(changes.isNotEmpty(), "no change found; has the scan stopped seeing them?")

        val undeclared = changes.filter { it.substringBeforeLast('.') !in declared }
        assertEquals(emptyList(), undeclared, "a change outside a declared stage never runs")
    }

    /** See [FlamingockConfig]: cut off midway without it, a change stops every later start until resolved by hand. */
    @Test
    fun `every change is run again after being cut off`() {
        val waitForHand = changes.filter {
            Class.forName(it).getAnnotation(Recovery::class.java)?.strategy != RecoveryStrategy.ALWAYS_RETRY
        }
        assertEquals(emptyList(), waitForHand, "make the change safe to run twice and mark it ALWAYS_RETRY")
    }
}
