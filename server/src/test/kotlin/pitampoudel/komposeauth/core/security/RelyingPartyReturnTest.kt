package pitampoudel.komposeauth.core.security

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RelyingPartyReturnTest {

    private val client: RegisteredClient = RegisteredClient.withId("registration-1")
        .clientId("rp-client")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://rp.example.com/callback")
        .build()

    private val clients = object : RegisteredClientRepository {
        override fun save(registeredClient: RegisteredClient) = Unit
        override fun findById(id: String) = client.takeIf { it.id == id }
        override fun findByClientId(clientId: String) = client.takeIf { it.clientId == clientId }
    }

    private val returnTo = RelyingPartyReturn(clients)

    private fun authorizeRequest(query: String): MockHttpServletRequest =
        MockHttpServletRequest("GET", "/oauth2/authorize").apply { queryString = query }

    /** A browser sending back the cookie an earlier response set. */
    private fun requestCarrying(response: MockHttpServletResponse): MockHttpServletRequest =
        MockHttpServletRequest("GET", "/login/oauth2/code/google").apply {
            setCookies(*response.cookies)
        }

    private val fullQuery = "response_type=code&client_id=rp-client&state=rp-state&redirect_uri=" +
            URLEncoder.encode("https://rp.example.com/callback", StandardCharsets.UTF_8)

    private fun remembered(query: String = fullQuery): MockHttpServletRequest {
        val response = MockHttpServletResponse()
        returnTo.remember(authorizeRequest(query), response)
        return requestCarrying(response)
    }

    @Test
    fun `an interrupted authorization request is replayed as it arrived`() {
        val resumed = returnTo.resume(remembered(), MockHttpServletResponse())

        assertEquals("/oauth2/authorize?$fullQuery", resumed)
    }

    @Test
    fun `nothing is remembered for a request that is not an authorization request`() {
        val response = MockHttpServletResponse()
        val elsewhere = MockHttpServletRequest("GET", "/oauth2/token").apply {
            queryString = "client_id=rp-client"
        }

        returnTo.remember(elsewhere, response)

        assertNull(returnTo.resume(requestCarrying(response), MockHttpServletResponse()))
    }

    @Test
    fun `a failure is returned to the relying party with the state it started with`() {
        val target = assertNotNull(
            returnTo.errorRedirect(remembered(), MockHttpServletResponse(), "access_denied")
        )

        assertEquals("https://rp.example.com/callback?error=access_denied&state=rp-state", target)
    }

    /**
     * The cookie is the visitor's to tamper with, so the redirect URI in it is only ever honoured
     * when the client has registered it — otherwise this would forward anyone anywhere on the say-so
     * of a value they control.
     */
    @Test
    fun `a redirect URI the client has not registered is refused`() {
        val forged = "response_type=code&client_id=rp-client&state=rp-state&redirect_uri=" +
                URLEncoder.encode("https://attacker.example.com/collect", StandardCharsets.UTF_8)

        assertNull(returnTo.errorRedirect(remembered(forged), MockHttpServletResponse(), "server_error"))
    }

    @Test
    fun `an unknown client is refused`() {
        val unknown = "response_type=code&client_id=not-registered&state=rp-state"

        assertNull(returnTo.errorRedirect(remembered(unknown), MockHttpServletResponse(), "server_error"))
    }

    /**
     * Left in place rather than spent, so that a visitor who signs in from the login page we fell
     * back to is still returned to the application afterwards.
     */
    @Test
    fun `a refused error redirect keeps the record for the sign-in that follows`() {
        val request = remembered("response_type=code&client_id=not-registered")
        val response = MockHttpServletResponse()

        returnTo.errorRedirect(request, response, "server_error")

        assertNotNull(returnTo.resume(request, MockHttpServletResponse()))
    }

    @Test
    fun `a spent record does not divert the next sign-in`() {
        val request = remembered()
        val response = MockHttpServletResponse()

        assertNotNull(returnTo.resume(request, response))

        val cleared = assertNotNull(response.getCookie("authorize_request"))
        assertEquals(0, cleared.maxAge)
        assertTrue(cleared.value.isNullOrEmpty())
    }
}
