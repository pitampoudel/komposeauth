package pitampoudel.komposeauth.core.utils

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Reading a verified Google payload, with attention to the claims that are not always there.
 *
 * Only `sub` and `email` can be counted on. Google documents the name claims as present "when a name
 * claim is present", and treating them as guaranteed is what turned a normal sign-in into a
 * whitelabel 500 — thrown inside the sign-in filter, where no exception handler could reach it, on a
 * callback URL that said nothing about which claim was missing.
 */
class GoogleProfileFromTest {

    private fun payload(vararg claims: Pair<String, Any>) = GoogleIdToken.Payload().apply {
        claims.forEach { (key, value) -> set(key, value) }
    }

    @Test
    fun `reads a fully populated payload`() {
        val profile = googleProfileFrom(
            payload(
                "email" to "ada@example.com",
                "given_name" to "Ada",
                "family_name" to "Lovelace",
                "picture" to "https://example.com/ada.png"
            )
        )

        assertEquals("ada@example.com", profile.email)
        assertEquals("Ada", profile.firstName)
        assertEquals("Lovelace", profile.lastName)
        assertEquals("https://example.com/ada.png", profile.photoUrl)
    }

    @Test
    fun `accepts an account with no given name`() {
        // The regression. This threw, and the visitor saw a 500 on the OAuth callback.
        val profile = googleProfileFrom(
            payload("email" to "ada@example.com", "name" to "Ada Lovelace")
        )

        assertEquals("ada@example.com", profile.email)
        // Falls back to the display name rather than leaving the account nameless.
        assertEquals("Ada Lovelace", profile.firstName)
        assertNull(profile.lastName)
    }

    @Test
    fun `accepts an account carrying no name at all`() {
        val profile = googleProfileFrom(payload("email" to "ada@example.com"))

        assertEquals("ada@example.com", profile.email)
        assertNull(profile.firstName)
        assertNull(profile.lastName)
        assertNull(profile.photoUrl)
    }

    @Test
    fun `accepts an account with no picture`() {
        val profile = googleProfileFrom(
            payload("email" to "ada@example.com", "given_name" to "Ada")
        )

        assertNull(profile.photoUrl)
        assertEquals("Ada", profile.firstName)
    }

    @Test
    fun `refuses a payload with no email, since an account cannot be keyed without one`() {
        val thrown = assertThrows<IllegalArgumentException> {
            googleProfileFrom(payload("given_name" to "Ada"))
        }

        // The message is the whole point: the old failure named nothing.
        assertEquals(true, thrown.message?.contains("email", ignoreCase = true))
    }
}
