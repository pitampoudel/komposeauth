package pitampoudel.komposeauth.core.config

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoogleAuthorizationRequestResolverTest {

    private val clientRegistrationRepository = InMemoryClientRegistrationRepository(
        CommonOAuth2Provider.GOOGLE.getBuilder("google")
            .clientId("client-id")
            .clientSecret("client-secret")
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .build()
    )

    private val resolver = GoogleAuthorizationRequestResolver(
        DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository,
            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
        )
    )

    private fun request(query: String = ""): MockHttpServletRequest =
        MockHttpServletRequest("GET", "/oauth2/authorization/google").apply {
            query.removePrefix("?").split("&").filter { it.isNotBlank() }.forEach {
                val (name, value) = it.split("=", limit = 2)
                setParameter(name, value)
            }
        }

    @Test
    fun `defaults to select_account so the user can switch google account`() {
        val authorizationRequest = assertNotNull(resolver.resolve(request()))

        assertEquals("select_account", authorizationRequest.additionalParameters["prompt"])
        assertTrue(authorizationRequest.authorizationRequestUri.contains("prompt=select_account"))
    }

    @Test
    fun `forwards a prompt google understands`() {
        val authorizationRequest = assertNotNull(resolver.resolve(request("?prompt=consent")))

        assertEquals("consent", authorizationRequest.additionalParameters["prompt"])
    }

    @Test
    fun `maps an unsupported prompt back to the default`() {
        // `login` is valid OIDC on our own authorization endpoint, but Google does not accept it.
        val authorizationRequest = assertNotNull(resolver.resolve(request("?prompt=login")))

        assertEquals("select_account", authorizationRequest.additionalParameters["prompt"])
    }

    @Test
    fun `forwards login_hint when provided`() {
        val authorizationRequest =
            assertNotNull(resolver.resolve(request("?login_hint=someone@example.com")))

        assertEquals("someone@example.com", authorizationRequest.additionalParameters["login_hint"])
        assertTrue(authorizationRequest.authorizationRequestUri.contains("login_hint="))
    }

    @Test
    fun `omits login_hint when absent`() {
        val authorizationRequest = assertNotNull(resolver.resolve(request()))

        assertNull(authorizationRequest.additionalParameters["login_hint"])
    }

    @Test
    fun `a request that is not an authorization request resolves to null`() {
        val other = MockHttpServletRequest("GET", "/session-login")

        assertNull(resolver.resolve(other))
    }

    @Test
    fun `prompt mapping keeps only google supported values`() {
        assertEquals("select_account", GoogleAuthorizationRequestResolver.googlePrompt(null))
        assertEquals("select_account", GoogleAuthorizationRequestResolver.googlePrompt("login"))
        assertEquals("none", GoogleAuthorizationRequestResolver.googlePrompt("none"))
        assertEquals(
            "consent select_account",
            GoogleAuthorizationRequestResolver.googlePrompt("consent login select_account")
        )
    }
}
