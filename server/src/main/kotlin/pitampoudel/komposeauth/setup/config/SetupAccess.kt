package pitampoudel.komposeauth.setup.config

import org.springframework.stereotype.Component
import pitampoudel.komposeauth.setup.entity.Env
import pitampoudel.komposeauth.setup.repository.EnvRepository


@Component("setupAccess")
class SetupAccess(
    private val repo: EnvRepository,
) {
    fun isOpen(): Boolean {
        return !repo.existsById(Env.SINGLETON_ID)
    }
}
