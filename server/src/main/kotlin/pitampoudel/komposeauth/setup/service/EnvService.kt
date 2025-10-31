package pitampoudel.komposeauth.setup.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.setup.entity.Env
import pitampoudel.komposeauth.setup.repository.EnvRepository
import java.util.concurrent.atomic.AtomicReference

@Service
class EnvService(
    private val repo: EnvRepository
) {
    private val cache = AtomicReference<Env?>(null)

    fun getEnv(): Env {
        val cached = cache.get()
        if (cached != null) return cached
        val loaded = repo.findById(Env.SINGLETON_ID).orElse(Env())
        cache.set(loaded)
        return loaded
    }

    fun save(env: Env): Env {
        val saved = repo.save(env.copy(id = Env.SINGLETON_ID))
        cache.set(saved)
        return saved
    }

    fun update(block: (Env) -> Env): Env {
        return save(block(getEnv()))
    }

    fun clearCache() {
        cache.set(null)
    }
}