package pitampoudel.core.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailValidationTest {

    @Test
    fun `real addresses are accepted`() {
        listOf("name+tag@gmail.com", "a.b-c@mail.example.company", "x@example.travel", "Upper@Example.COM")
            .forEach { assertTrue(it.isValidEmail(), it) }
    }

    @Test
    fun `malformed addresses are refused`() {
        listOf("plain", "a@b", "a b@example.com", "@example.com", "a@example.c", "a@@example.com", null)
            .forEach { assertFalse(it.isValidEmail(), it.toString()) }
    }
}
