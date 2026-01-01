package pitampoudel.komposeauth.core.migrations

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class DbMigrationRunnerIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private class TestMigration(
        override val migrationId: String,
        override val fromSchemaVersion: Int,
        val body: (MongoTemplate) -> Unit
    ) : DbMigration {
        override fun run(mongoTemplate: MongoTemplate) = body(mongoTemplate)
    }

    @Test
    fun `migrateIfNeeded is idempotent via migration journal`() {
        mongoTemplate.dropCollection("schema_state")
        mongoTemplate.dropCollection("schema_migrations")
        mongoTemplate.dropCollection("locks")
        mongoTemplate.dropCollection("test_items")

        val migration = TestMigration(
            migrationId = "test-001",
            fromSchemaVersion = 0
        ) { template ->
            template.insert(mapOf("_id" to "1", "value" to "hello"), "test_items")
        }

        val runner = DbMigrationRunner(mongoTemplate, listOf(migration))
        runner.migrateIfNeeded()
        runner.migrateIfNeeded()

        val schemaVersion = mongoTemplate.findById("singleton", SchemaState::class.java)?.version
        assertEquals(1, schemaVersion)

        val applied = mongoTemplate.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("migrationId").`is`("test-001")
            ),
            MigrationRun::class.java
        )
        // We only guarantee it's applied once successfully.
        assertTrue(applied.any { it.status == MigrationRun.Status.SUCCESS })

        val doc = mongoTemplate.findById("1", Map::class.java, "test_items")
        assertNotNull(doc)
    }

    @Test
    fun `concurrent runners - only one acquires lock`() {
        mongoTemplate.dropCollection("schema_state")
        mongoTemplate.dropCollection("schema_migrations")
        mongoTemplate.dropCollection("locks")
        mongoTemplate.dropCollection("test_items")

        val latch = CountDownLatch(1)

        val slowMigration = TestMigration(
            migrationId = "test-slow",
            fromSchemaVersion = 0
        ) { template ->
            // Ensure the other runner has a chance to contend for the lock.
            latch.countDown()
            Thread.sleep(1500)
            template.insert(mapOf("_id" to "x", "ok" to true), "test_items")
        }

        val runner1 = DbMigrationRunner(mongoTemplate, listOf(slowMigration))
        val runner2 = DbMigrationRunner(mongoTemplate, listOf(slowMigration))

        val pool = Executors.newFixedThreadPool(2)
        try {
            val f1 = pool.submit<Unit> { runner1.migrateIfNeeded() }

            // Wait until runner1 is inside migration body.
            assertTrue(latch.await(5, TimeUnit.SECONDS))

            val f2 = pool.submit<Unit> {
                // Depending on timing, runner2 can fail to acquire the lock OR
                // it can acquire the lock after a lease expiry and then hit the unique journal insert.
                try {
                    runner2.migrateIfNeeded()
                    throw AssertionError("Expected runner2 to fail due to lock/journal contention")
                } catch (_: IllegalStateException) {
                    // expected
                } catch (_: DuplicateKeyException) {
                    // expected
                }
            }

            f1.get(10, TimeUnit.SECONDS)
            f2.get(10, TimeUnit.SECONDS)

            val schemaVersion = mongoTemplate.findById("singleton", SchemaState::class.java)?.version
            assertEquals(1, schemaVersion)

            val lockCount = mongoTemplate.getCollection("locks").countDocuments()
            assertEquals(0L, lockCount)
        } finally {
            pool.shutdownNow()
        }
    }
}
