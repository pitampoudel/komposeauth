package pitampoudel.komposeauth.core.utils

import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet.OAuth2AuthorizationServerAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AnyRequestMatcher
import java.util.function.Supplier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ServerUrlTest {

    private fun serverUrl(issuer: String?) =
        ServerUrl(AuthorizationServerSettings.builder().apply { issuer?.let { issuer(it) } }.build())

    @Test
    fun `the configured issuer is the address, exactly as written`() {
        assertEquals("https://auth.example.com", serverUrl("https://auth.example.com").value)
        assertEquals("http://localhost:8080", serverUrl("http://localhost:8080").value)
    }

    @Test
    fun `without an issuer there is no address, and the refusal names the setting`() {
        for (missing in listOf(null, "", "  ")) {
            val refusal = assertFailsWith<IllegalStateException> { serverUrl(missing) }
            assertTrue(ServerUrl.PROPERTY in refusal.message.orEmpty(), refusal.message)
            assertTrue(ServerUrl.ENV_VAR in refusal.message.orEmpty(), refusal.message)
        }
    }

    @Test
    fun `an issuer that is not an origin alone is refused`() {
        listOf(
            "https://auth.example.com/",
            "https://auth.example.com/auth",
            "https://auth.example.com?tenant=a",
            "https://auth.example.com#top",
            "https://user@auth.example.com",
            "auth.example.com",
            "ftp://auth.example.com",
            "https://",
            "https://auth.example.com ",
        ).forEach { issuer ->
            val refusal = assertFailsWith<IllegalStateException>(issuer) { serverUrl(issuer) }
            assertTrue(ServerUrl.ENV_VAR in refusal.message.orEmpty(), refusal.message)
        }
    }

    /**
     * Through Boot's own binding of the property, as the server reads it at startup. The beans are
     * handed to the runner rather than declared in a configuration class, which component scanning
     * would also find in every other test's context.
     */
    private val contextRunner = WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(OAuth2AuthorizationServerAutoConfiguration::class.java))
        .withUserConfiguration(ServerUrl::class.java)
        // Present only so Boot's default authorization server security stays out of a context
        // that has no HttpSecurity to build it with.
        .withBean(SecurityFilterChain::class.java, Supplier<SecurityFilterChain> { DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE) })
        .withInitializer { ctx ->
            ctx.environment.propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)
        }

    @Test
    fun `the server does not start without an issuer`() {
        contextRunner.run { ctx ->
            val failure = assertNotNull(ctx.startupFailure, "started with no issuer")
            val cause = generateSequence(failure) { it.cause }.last()
            assertTrue(cause is IllegalStateException && ServerUrl.ENV_VAR in cause.message.orEmpty(), "$cause")
        }
    }

    @Test
    fun `the server does not start with a malformed issuer`() {
        contextRunner
            .withPropertyValues("${ServerUrl.PROPERTY}=https://auth.example.com/")
            .run { ctx -> assertNotNull(ctx.startupFailure, "started with a trailing slash on the issuer") }
    }

    @Test
    fun `the environment variable is how the issuer is set`() {
        contextRunner
            .withInitializer { ctx ->
                ctx.environment.propertySources.addFirst(
                    SystemEnvironmentPropertySource(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        mapOf<String, Any>(ServerUrl.ENV_VAR to "https://auth.example.com")
                    )
                )
            }
            .run { ctx ->
                assertEquals(null, ctx.startupFailure)
                assertEquals("https://auth.example.com", ctx.getBean(ServerUrl::class.java).value)
            }
    }
}
