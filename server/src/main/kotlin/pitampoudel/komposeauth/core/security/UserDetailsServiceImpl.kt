package pitampoudel.komposeauth.core.security

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.user.repository.UserRepository

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUserName(username)
            ?: throw UsernameNotFoundException("User not found: $username")
        if (user.passwordHash == null) {
            throw UsernameNotFoundException("No password set for user: $username")
        }
        val authorities = user.roles.map { SimpleGrantedAuthority("ROLE_$it") }
        return User.withUsername(user.id.toHexString())
            .password(user.passwordHash)
            .authorities(authorities)
            .accountLocked(user.deactivated)
            .build()
    }
}