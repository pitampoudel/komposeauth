package pitampoudel.komposeauth.setup.config

import org.springframework.stereotype.Component
import pitampoudel.komposeauth.user.repository.UserRepository


@Component("setupAccess")
class SetupAccess(
    private val userRepository: UserRepository,
) {
    fun isOpen(): Boolean {
        // If no user exists yet, allow anyone access to setup
        return userRepository.count().toInt() == 0
    }
}
