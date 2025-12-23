package pitampoudel.komposeauth.core.providers

import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import kotlin.test.Test
import kotlin.test.assertEquals

class OAuth2PublicClientAuthConverterTest {

    private val sut = OAuth2PublicClientAuthConverter()

    @Test
    fun `throws invalid_request when client_id missing`() {
        val ex = assertThrows<OAuth2AuthenticationException> {
            sut.convert(MockHttpServletRequest())
        }
        assertEquals(OAuth2ErrorCodes.INVALID_REQUEST, ex.error.errorCode)
    }

    @Test
    fun `converts request to OAuth2PublicClientAuthToken`() {
        val request = MockHttpServletRequest().apply {
            addParameter(OAuth2ParameterNames.CLIENT_ID, "public-client")
        }
        val auth = sut.convert(request) as OAuth2PublicClientAuthToken
        assertEquals("public-client", auth.clientId)
    }
}

