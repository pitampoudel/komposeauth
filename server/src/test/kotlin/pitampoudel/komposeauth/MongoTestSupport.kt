package pitampoudel.komposeauth

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

/**
 * Centralized Testcontainers MongoDB support for tests.
 *
 * Contract:
 * - Starts a single MongoDB container lazily (once per JVM).
 * - Exposes a Spring [ApplicationContextInitializer] to inject Spring Data Mongo properties.
 * - Avoids duplicated @DynamicPropertySource blocks across test classes.
 */
internal object MongoTestSupport {

    // Stable 32-byte AES key (base64) for tests.
    internal const val TEST_BASE64_ENCRYPTION_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    private val image: DockerImageName = DockerImageName.parse("mongo:5.0")

    internal val mongo: MongoDBContainer by lazy {
        MongoDBContainer(image)
            // speeds up startup for replicaSetUrl
            .withReuse(false)
            .also { it.start() }
    }

    internal fun mongoUri(): String = mongo.replicaSetUrl

    /**
     * Spring context initializer that wires container properties early enough for auto-configuration.
     */
    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            val uri = mongoUri()
            TestPropertyValues.of(
                "spring.data.mongodb.uri=$uri",
                "spring.data.mongodb.database=test",
                "app.base64-encryption-key=$TEST_BASE64_ENCRYPTION_KEY",
            ).applyTo(applicationContext.environment)
        }
    }
}

