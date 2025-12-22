package pitampoudel.komposeauth

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.function.Supplier

/**
 * Base class for tests that require a running MongoDB container.
 * Simply extend to inherit a spring-managed mongo repository
 */
@SpringBootTest
@Testcontainers
abstract class MongoContainerTest {

    companion object {

        @Container
        @JvmStatic
        internal val mongoDBContainer: MongoDBContainer = MongoDBContainer("mongo:latest")

        @DynamicPropertySource
        @JvmStatic
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri", Supplier { mongoDBContainer.replicaSetUrl })
            registry.add("spring.data.mongodb.database", { "test" })
        }
    }
}
