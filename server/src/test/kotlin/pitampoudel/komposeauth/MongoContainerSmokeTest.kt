package pitampoudel.komposeauth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals

/**
 * Guards the centralized Mongo Testcontainers setup.
 *
 * If this fails, it usually means the Spring context didn't receive the
 * container-backed `spring.data.mongodb.uri` property.
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
@AutoConfigureMockMvc
class MongoContainerSmokeTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Test
    fun `mongo template can connect and roundtrip a simple document`() {
        val collection = "smoke_test"
        mongoTemplate.dropCollection(collection)

        mongoTemplate.insert(mapOf("_id" to "1", "hello" to "world"), collection)

        val doc = mongoTemplate.findById("1", Map::class.java, collection)
        assertEquals(doc?.get("hello"), "world")
    }
}

