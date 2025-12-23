package pitampoudel.komposeauth.core.providers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import java.util.UUID

class OAuth2PublicClientAuthProviderTest {

    private class FakeRegisteredClientRepository(
        private val clientsById: Map<String, RegisteredClient>
    ) : RegisteredClientRepository {
        override fun save(registeredClient: RegisteredClient) {
            throw UnsupportedOperationException("not needed for test")
        }

        override fun findById(id: String): RegisteredClient? = clientsById.values.firstOrNull { it.id == id }

        override fun findByClientId(clientId: String): RegisteredClient? = clientsById[clientId]
    }

    @Test
    fun `supports OAuth2PublicClientAuthToken`() {
        val repo = FakeRegisteredClientRepository(emptyMap())
        val sut = OAuth2PublicClientAuthProvider(repo)

        assertTrue(sut.supports(OAuth2PublicClientAuthToken::class.java))
    }

    @Test
    fun `rejects unknown client id`() {
        val repo = FakeRegisteredClientRepository(emptyMap())
        val sut = OAuth2PublicClientAuthProvider(repo)

        val ex = assertThrows<OAuth2AuthenticationException> {
            sut.authenticate(OAuth2PublicClientAuthToken("missing"))
        }
        assertEquals(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, ex.error.errorCode)
    }

    @Test
    fun `authenticates public client when registered`() {
        val registered = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("client")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://example.com")
            .scope("openid")
            .build()

        val repo = FakeRegisteredClientRepository(mapOf("client" to registered))
        val sut = OAuth2PublicClientAuthProvider(repo)

        val result = sut.authenticate(OAuth2PublicClientAuthToken("client"))
            as org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken

        assertEquals("client", result.registeredClient?.clientId)
        assertEquals(ClientAuthenticationMethod.NONE, result.clientAuthenticationMethod)
    }
}
