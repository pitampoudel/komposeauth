package pitampoudel.komposeauth.core

import org.bson.Document
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.migration._0001__DropOldCollectionsChange
import kotlin.test.assertTrue

/** The settings collection from before November 2025 and the old runners' records are gone. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class DropOldCollectionsIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Test
    fun `the old collections are dropped`() {
        val old = listOf("env", "schema_migrations", "schema_state", "locks")
        old.forEach { mongoTemplate.getCollection(it).insertOne(Document("_id", "old")) }

        val change = _0001__DropOldCollectionsChange()
        change.apply(mongoTemplate)
        change.apply(mongoTemplate)

        val left = mongoTemplate.collectionNames
        assertTrue(old.none { it in left }, "left: $left")
    }
}
