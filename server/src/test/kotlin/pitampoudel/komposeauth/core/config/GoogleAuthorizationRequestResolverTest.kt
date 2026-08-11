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

    private fun request(vararg parameters: Pair<String, String>): MockHttpServletRequest =
        MockHttpServletRequest("GET", "/oauth2/authorization/google").apply {
            parameters.forEach { (name, value) -> setParameter(name, value) }
        }

    @Test
    fun `asks for the account chooser so the user can switch google account`() {
        val authorizationRequest = assertNotNull(resolver.resolve(request()))

        assertEquals("select_account", authorizationRequest.additionalParameters["prompt"])
        assertTrue(authorizationRequest.authorizationRequestUri.contains("prompt=select_account"))
    }

    @Test
    fun `forwards login_hint when provided`() {
        val authorizationRequest =
            assertNotNull(resolver.resolve(request("login_hint" to "someone@example.com")))

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
}
