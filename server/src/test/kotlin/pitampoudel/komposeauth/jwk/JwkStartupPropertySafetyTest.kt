package pitampoudel.komposeauth.jwk

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pitampoudel.komposeauth.StaticAppProperties
import pitampoudel.komposeauth.core.service.security.CryptoService

/**
 * Production-safety checks for encryption-key configuration.
 *
 * Goal: JWK private keys must never be stored/loaded without a valid AES key.
 */
class JwkStartupPropertySafetyTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration::class.java))
        .withUserConfiguration(TestConfig::class.java)

    @Configuration
    internal class TestConfig {
        @Bean
        fun props(): StaticAppProperties = StaticAppProperties()

        @Bean
        fun crypto(props: StaticAppProperties) = CryptoService(props)
    }

    @Test
    fun `context fails when app base64 encryption key is missing`() {
        // StaticAppProperties.base64EncryptionKey is lateinit, so this should fail on CryptoService init.
        contextRunner.run { ctx ->
            assertThrows(Exception::class.java) {
                ctx.getBean(CryptoService::class.java)
            }
        }
    }

    @Test
    fun `context fails when app base64 encryption key is not base64`() {
        contextRunner
            .withPropertyValues("app.base64-encryption-key=not-base64!!!")
            .run { ctx ->
                assertThrows(IllegalStateException::class.java) {
                    ctx.getBean(CryptoService::class.java)
                }
            }
    }

    @Test
    fun `context fails when app base64 encryption key is wrong length`() {
        // 15 bytes when decoded -> invalid
        contextRunner
            .withPropertyValues("app.base64-encryption-key=AQIDBAUGBwgJCgsMDQ4=")
            .run { ctx ->
                assertThrows(IllegalStateException::class.java) {
                    ctx.getBean(CryptoService::class.java)
                }
            }
    }
}

