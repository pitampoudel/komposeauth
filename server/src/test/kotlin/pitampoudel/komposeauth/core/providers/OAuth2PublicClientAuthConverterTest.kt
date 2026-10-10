package pitampoudel.komposeauth.core.providers

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OAuth2PublicClientAuthConverterTest {

    private val sut = OAuth2PublicClientAuthConverter()

    private fun request(vararg params: Pair<String, String>) = MockHttpServletRequest("POST", "/oauth2/token").apply {
        addParameter(OAuth2ParameterNames.CLIENT_ID, "public-client")
        params.forEach { (name, value) -> addParameter(name, value) }
    }

    @Test
    fun `converts request to OAuth2PublicClientAuthToken`() {
        val auth = sut.convert(request(OAuth2ParameterNames.GRANT_TYPE to "refresh_token")) as OAuth2PublicClientAuthToken
        assertEquals("public-client", auth.clientId)
    }

    @Test
    fun `leaves a request with a client secret to Spring`() {
        // Taking it here would ignore the secret and authenticate the client as public.
        assertNull(
            sut.convert(
                request(
                    OAuth2ParameterNames.GRANT_TYPE to "refresh_token",
                    OAuth2ParameterNames.CLIENT_SECRET to "s3cret"
                )
            )
        )
    }

    @Test
    fun `leaves a request with basic auth to Spring`() {
        val credentials = Base64.getEncoder().encodeToString("public-client:not-a-real-secret".toByteArray())
        val request = request(OAuth2ParameterNames.GRANT_TYPE to "refresh_token").apply {
            addHeader("Authorization", "Basic $credentials")
        }
        assertNull(sut.convert(request))
    }

    @Test
    fun `leaves the code exchange to Spring, which checks the PKCE verifier`() {
        // Regression: claimed here, a code was redeemed with neither a verifier nor a secret.
        assertNull(sut.convert(request(OAuth2ParameterNames.GRANT_TYPE to "authorization_code", OAuth2ParameterNames.CODE to "c")))
    }

    @Test
    fun `never authenticates client_credentials by client id alone`() {
        // A client id ships inside every app; this would let anyone mint service tokens.
        assertNull(sut.convert(request(OAuth2ParameterNames.GRANT_TYPE to "client_credentials")))
    }
}
