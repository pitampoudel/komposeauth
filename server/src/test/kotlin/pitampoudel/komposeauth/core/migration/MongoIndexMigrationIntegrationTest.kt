package pitampoudel.komposeauth.core.migration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.webauthn.entity.PublicKeyUser
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the indexes still being there after `spring.data.mongodb.auto-index-creation` was turned
 * off in favour of [MongoIndexMigration].
 *
 * The point of that swap was to keep a cold start from re-issuing thirty `createIndex` commands, and
 * the way it could go wrong is silently: nothing fails when an index is missing, queries just get
 * slower and a uniqueness constraint that was being relied on stops holding. So assert the indexes
 * exist rather than that the migration ran.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class MongoIndexMigrationIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var migration: MongoIndexMigration

    @Autowired
    private lateinit var mappingContext: org.springframework.data.mongodb.core.mapping.MongoMappingContext

    @Test
    fun `spring data is not also creating indexes on every refresh`() {
        // The saving this whole arrangement exists for is only real if the property actually took
        // effect. Boot 4 moved the MongoDB connection properties to `spring.mongodb.*` and left this
        // one under `spring.data.mongodb.*`, so "the indexes are present" on its own would prove
        // nothing -- they would be present either way, just built by the path being replaced.
        assertTrue(
            !mappingContext.isAutoIndexCreation,
            "spring.data.mongodb.auto-index-creation is still on; MongoIndexMigration is redundant " +
                    "and every cold start is still re-issuing createIndex for each mapped type"
        )
    }

    private fun indexedFieldsOf(type: Class<*>): Set<String> =
        mongoTemplate.indexOps(type).indexInfo
            .flatMap { info -> info.indexFields.map { it.key } }
            .toSet()

    @Test
    fun `unique indexes declared by annotations exist on the collection`() {
        // PublicKeyUser carries three separate @Indexed(unique = true) fields; User's uniqueness
        // comes from @CompoundIndexes. Between them they cover both resolution paths.
        val credentialUserIndexes = indexedFieldsOf(PublicKeyUser::class.java)
        assertTrue(
            credentialUserIndexes.size > 1,
            "expected the annotated indexes on PublicKeyUser, found $credentialUserIndexes"
        )

        val userIndexes = indexedFieldsOf(User::class.java)
        assertTrue(
            userIndexes.size > 1,
            "expected the compound indexes on User, found $userIndexes"
        )
    }

    @Test
    fun `ttl indexes exist, so expiring collections still prune themselves`() {
        // These are the ones with no other symptom when missing: rate-limit windows and OAuth
        // authorizations would simply accumulate forever.
        val rateLimitTtl = mongoTemplate.indexOps("rate_limits").indexInfo
            .any { it.expireAfter.isPresent }
        assertTrue(rateLimitTtl, "rate_limits lost its TTL index")
    }

    @Test
    fun `the migration id is stable across calls`() {
        // MongoMigrationRunner reads the id to reserve the run and again to release it on failure.
        // An id derived from an unsorted scan would differ between the two, orphaning the
        // reservation and making the migration never run again.
        assertEquals(migration.id, migration.id)
        assertTrue(migration.id.startsWith("mongo-indexes-"), migration.id)
    }
}
