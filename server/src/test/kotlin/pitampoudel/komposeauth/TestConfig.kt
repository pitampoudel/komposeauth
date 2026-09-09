package pitampoudel.komposeauth

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.mongodb.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import java.util.Base64
import javax.crypto.KeyGenerator

@TestConfiguration(proxyBeanMethods = false)
class TestConfig {
    companion object {
        val testKey: String by lazy {
            val kg = KeyGenerator.getInstance("AES").apply { init(256) }
            Base64.getEncoder().encodeToString(kg.generateKey().encoded)
        }

        init {
            // Set as system property immediately when class loads
            System.setProperty("app.base64-encryption-key", testKey)
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProps(registry: DynamicPropertyRegistry) {
            registry.add("app.base64-encryption-key") { testKey }
        }
    }

    @Bean
    @ServiceConnection
    fun mongoDbContainer(): MongoDBContainer =
        MongoDBContainer(DockerImageName.parse("mongo:latest"))
}
