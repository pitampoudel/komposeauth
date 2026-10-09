package pitampoudel.komposeauth.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthUserTest {
    private fun user(givenName: String?, familyName: String?) = AuthUser(
        authorities = emptyList(),
        givenName = givenName,
        familyName = familyName,
        kycVerified = false,
        phoneNumberVerified = false,
        sub = "1"
    )

    @Test
    fun fullNameJoinsTheNamesThatExist() {
        assertEquals("Ada Lovelace", user("Ada", "Lovelace").fullName())
        assertEquals("Lovelace", user(null, "Lovelace").fullName())
        assertEquals("Ada", user("Ada", null).fullName())
        assertEquals("", user(null, null).fullName())
    }
}
