package pitampoudel.komposeauth.authorization

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.web.authentication.WebAuthenticationDetails
import java.io.File
import java.io.InvalidClassException
import java.security.Principal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/** The attributes are stored with Java serialization, so reading them back takes only known classes. */
class AuthorizationAttributesTest {

    private val objectMapper = ObjectMapper()

    private val client = RegisteredClient.withId("client")
        .clientId("client")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://app.example/callback")
        .scope("openid")
        .build()

    private fun authorization(vararg attributes: Pair<String, Any>) = OAuth2Authorization.withRegisteredClient(client)
        .id("authorization-1")
        .principalName("user-1")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .attributes { it.putAll(attributes) }
        .build()

    private fun roundTrip(authorization: OAuth2Authorization) =
        fromOAuth2AuthorizationDocument(toOAuth2AuthorizationDocument(authorization, objectMapper), client, objectMapper)

    @Test
    fun `what a sign-in stores comes back as it was`() {
        val principal = UsernamePasswordAuthenticationToken.authenticated(
            "user-1", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
        ).apply { details = WebAuthenticationDetails("203.0.113.7", "session-1") }
        val request = OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://auth.example/oauth2/authorize")
            .clientId("client")
            .redirectUri("https://app.example/callback")
            .scopes(setOf("openid"))
            .state("state-1")
            .additionalParameters(mapOf("code_challenge" to "challenge", "code_challenge_method" to "S256"))
            .build()

        val restored = assertNotNull(
            roundTrip(
                authorization(
                    Principal::class.java.name to principal,
                    OAuth2AuthorizationRequest::class.java.name to request
                )
            )
        )

        assertEquals(principal, restored.getAttribute(Principal::class.java.name))
        val restoredRequest = assertNotNull(restored.getAttribute<OAuth2AuthorizationRequest>(OAuth2AuthorizationRequest::class.java.name))
        assertEquals(request.additionalParameters, restoredRequest.additionalParameters)
        assertEquals(request.redirectUri, restoredRequest.redirectUri)
    }

    @Test
    fun `a class nothing here stores is refused`() {
        assertFailsWith<InvalidClassException> { roundTrip(authorization("planted" to File("/tmp"))) }
    }
}
