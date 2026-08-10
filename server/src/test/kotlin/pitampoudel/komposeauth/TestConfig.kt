package pitampoudel.komposeauth

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
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

    /**
     * Gives every MockMvc request a valid CSRF token.
     *
     * These tests authenticate with the access-token cookie, which is exactly the ambient-authority
     * path CSRF protection guards, so without a token every state-changing call is correctly
     * rejected. A real browser gets the token from the page it was served; tests have no page, so it
     * is supplied here rather than repeated at ~40 call sites. That the protection actually rejects
     * an untokened request is asserted directly in CsrfProtectionIntegrationTest.
     */
    @Bean
    fun csrfTokenOnEveryRequest(): MockMvcBuilderCustomizer = MockMvcBuilderCustomizer { builder ->
        // The customizer hands over a star-projected builder, so the self-type on defaultRequest
        // can't be inferred; the cast only names a type the builder already satisfies.
        @Suppress("UNCHECKED_CAST")
        val configurable = builder as ConfigurableMockMvcBuilder<DefaultMockMvcBuilder>
        configurable.defaultRequest<DefaultMockMvcBuilder>(
            MockMvcRequestBuilders.get("/").with(SecurityMockMvcRequestPostProcessors.csrf())
        )
    }
}
