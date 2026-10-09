package pitampoudel.komposeauth.authorization

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OAuth2AuthorizationDocumentTest {

    private fun document(state: String? = null, codeExpiresAt: Instant? = null) = OAuth2AuthorizationDocument(
        id = "a1",
        registeredClientId = "client",
        principalName = "user",
        authorizationGrantType = "authorization_code",
        state = state,
        authorizationCodeValue = codeExpiresAt?.let { "code" },
        authorizationCodeExpiresAt = codeExpiresAt
    )

    @Test
    fun `an authorization waiting for consent expires rather than staying forever`() {
        val expiresAt = assertNotNull(document(state = "s").expiresAt)
        val left = Duration.between(Instant.now(), expiresAt)
        assertTrue(left > Duration.ofMinutes(9) && left <= Duration.ofMinutes(10), "$left")
    }

    @Test
    fun `once it has a token it expires with that token`() {
        val codeExpiresAt = Instant.now().plusSeconds(300)
        assertEquals(codeExpiresAt, document(state = "s", codeExpiresAt = codeExpiresAt).expiresAt)
    }
}
