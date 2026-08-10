package pitampoudel.komposeauth.core.domain

object Roles {
    const val ADMIN = "ADMIN"
    const val SUPER_ADMIN = "SUPER_ADMIN"

    /** Roles the server itself relies on. Always grantable, whatever the configured catalog says. */
    val BUILT_IN = listOf(ADMIN, SUPER_ADMIN)

    /** Roles whose last remaining holder may not be revoked, so nobody gets locked out. */
    val PROTECTED = setOf(ADMIN, SUPER_ADMIN)

    private val VALID_NAME = Regex("^[A-Z][A-Z0-9_]{0,63}$")

    fun normalize(role: String) = role.trim().uppercase().replace(' ', '_').replace('-', '_')

    fun isValidName(role: String) = VALID_NAME.matches(role)
}
