package pitampoudel.komposeauth

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.Base64
import java.util.function.Supplier
import javax.crypto.KeyGenerator

/**
 * Base class for tests that require a running MongoDB container.
 * Simply extend to inherit a spring-managed mongo repository
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
abstract class MongoContainerTest {

    companion object {
        fun generateKey(): String {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256) // or 128/192
            val key = keyGen.generateKey()
            return Base64.getEncoder().encodeToString(key.encoded)
        }

        @Container
        @JvmStatic
        internal val mongoDBContainer: MongoDBContainer = MongoDBContainer("mongo:latest")

        @DynamicPropertySource
        @JvmStatic
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri", Supplier { mongoDBContainer.replicaSetUrl })
            registry.add("spring.data.mongodb.database", { "test" })

            // Required by StaticAppProperties / CryptoService. Provide a deterministic test key.
            // (This is a 32-byte key base64-encoded.)
            registry.add("app.base64-encryption-key") { generateKey() }
        }
    }
}
