package pitampoudel.komposeauth.core.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Supports legacy plaintext secrets while defaulting to BCrypt for new encodes.
 * This keeps OAuth2 client secrets working without weakening existing BCrypt hashes.
 */
class LegacyAwarePasswordEncoder(
    private val bcrypt: BCryptPasswordEncoder = BCryptPasswordEncoder()
) : PasswordEncoder {

    override fun encode(rawPassword: CharSequence?): String? {
        return bcrypt.encode(rawPassword)
    }

    override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean {
        if (encodedPassword.isNullOrBlank()) return false
        val encoded = encodedPassword.trim()
        return when {
            encoded.startsWith("{bcrypt}") -> bcrypt.matches(rawPassword, encoded.removePrefix("{bcrypt}"))
            encoded.startsWith("{noop}") -> rawPassword.toString() == encoded.removePrefix("{noop}")
            else -> rawPassword.toString() == encoded
        }
    }

}
