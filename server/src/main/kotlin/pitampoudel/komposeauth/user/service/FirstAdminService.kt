package pitampoudel.komposeauth.user.service

import org.bson.types.ObjectId
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import pitampoudel.core.domain.isValidEmail
import pitampoudel.komposeauth.core.domain.Roles
import pitampoudel.komposeauth.core.utils.normalizedEmail
import pitampoudel.komposeauth.user.domain.validators.ValidatePassword
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.repository.UserRepository

/**
 * Makes the first super admin of a fresh server, from the configuration page opened with the master
 * key. Without it, the only way into the console by account was editing the database by hand.
 *
 * It works only while nobody holds [Roles.SUPER_ADMIN]: after that, roles are granted by a super
 * admin from the Users page, so the master key can't be used to add a second one quietly.
 */
@Service
class FirstAdminService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    sealed interface Result {
        /** [created] is false when the address already had an account, whose password is unchanged. */
        data class Done(val user: User, val created: Boolean) : Result
        data class Refused(val problem: String) : Result
    }

    fun isNeeded(): Boolean = userRepository.countByRolesContaining(Roles.SUPER_ADMIN) == 0L

    fun create(email: String?, name: String?, password: String?): Result {
        if (!isNeeded()) return Result.Refused("This server already has a super admin. Grant roles from the Users page.")
        val address = email?.normalizedEmail().orEmpty()
        if (!address.isValidEmail()) return Result.Refused("Enter the email address the admin will sign in with.")

        val existing = userRepository.findByEmail(address)
        if (existing != null) {
            if (existing.deactivated) return Result.Refused("The account for $address is deactivated.")
            val saved = userRepository.save(existing.copy(roles = (existing.roles + ADMIN_ROLES).distinct()))
            return Result.Done(saved, created = false)
        }

        if (password == null || !ValidatePassword(password).isSuccess()) {
            return Result.Refused("Choose a password of at least 8 characters.")
        }
        val (first, last) = splitName(name)
        val user = userRepository.insert(
            User(
                id = ObjectId(),
                firstName = first,
                lastName = last,
                email = address,
                phoneNumber = null,
                // Whoever holds the master key is vouching for the address; there may be no mail server yet to prove it.
                emailVerified = true,
                passwordHash = passwordEncoder.encode(password),
                roles = ADMIN_ROLES
            )
        )
        return Result.Done(user, created = true)
    }

    private fun splitName(name: String?): Pair<String?, String?> {
        val parts = name?.trim()?.split(Regex("\\s+"), limit = 2)?.filter { it.isNotEmpty() }.orEmpty()
        return parts.getOrNull(0) to parts.getOrNull(1)
    }

    private companion object {
        val ADMIN_ROLES = listOf(Roles.ADMIN, Roles.SUPER_ADMIN)
    }
}
