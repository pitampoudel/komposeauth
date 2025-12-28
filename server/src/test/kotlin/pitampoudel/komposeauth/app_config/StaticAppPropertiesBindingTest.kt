package pitampoudel.komposeauth.app_config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import pitampoudel.komposeauth.StaticAppProperties
import pitampoudel.komposeauth.TestConfig

@Import(TestConfig::class)
class StaticAppPropertiesBindingTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration::class.java))
        .withUserConfiguration(TestConfig::class.java)

    @Configuration
    @EnableConfigurationProperties(StaticAppProperties::class)
    internal class TestConfig

    @Test
    fun `binds base64EncryptionKey from kebab-case property`() {
        contextRunner
            .withPropertyValues("app.base64-encryption-key=abc123")
            .run { ctx ->
                val props = ctx.getBean(StaticAppProperties::class.java)
                assertEquals("abc123", props.base64EncryptionKey)
            }
    }

    @Test
    fun `binds base64EncryptionKey from camelCase property (relaxed binding)`() {
        contextRunner
            .withPropertyValues("app.base64EncryptionKey=xyz")
            .run { ctx ->
                val props = ctx.getBean(StaticAppProperties::class.java)
                assertEquals("xyz", props.base64EncryptionKey)
            }
    }

    @Test
    fun `fails binding when property is absent because it is lateinit`() {
        contextRunner.run { ctx ->
            val props = ctx.getBean(StaticAppProperties::class.java)
            assertThrows(UninitializedPropertyAccessException::class.java) {
                @Suppress("UNUSED_VARIABLE")
                val unused = props.base64EncryptionKey
            }
        }
    }

}
