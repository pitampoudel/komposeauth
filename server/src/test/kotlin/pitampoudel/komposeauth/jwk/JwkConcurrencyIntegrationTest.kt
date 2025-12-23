package pitampoudel.komposeauth.jwk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import pitampoudel.komposeauth.MongoTestSupport
import pitampoudel.komposeauth.jwk.repository.JwkRepository
import pitampoudel.komposeauth.jwk.service.JwkService
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [MongoTestSupport.Initializer::class])
class JwkConcurrencyIntegrationTest {

    @Autowired private lateinit var jwkRepository: JwkRepository
    @Autowired private lateinit var jwkService: JwkService

    @Test
    fun `concurrent loadOrCreateKeyPair calls converge on single persisted key`() {
        // Create a clean state for the default kid.
        jwkRepository.deleteAll(jwkRepository.findAll().filter { it.kid == "spring-boot-jwk" })

        val pool = Executors.newFixedThreadPool(2)
        try {
            val f1 = pool.submit(Callable { jwkService.loadOrCreateKeyPair() })
            val f2 = pool.submit(Callable { jwkService.loadOrCreateKeyPair() })

            val kp1 = f1.get(30, TimeUnit.SECONDS)
            val kp2 = f2.get(30, TimeUnit.SECONDS)

            val m1 = (kp1.public as RSAPublicKey).modulus
            val m2 = (kp2.public as RSAPublicKey).modulus
            assertEquals(m1, m2)

            val all = jwkRepository.findAll().filter { it.kid == "spring-boot-jwk" }
            assertTrue(all.size == 1)
        } finally {
            pool.shutdownNow()
        }
    }
}

