package pitampoudel.komposeauth.user

import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import pitampoudel.komposeauth.user.entity.OneTimeToken
import pitampoudel.komposeauth.user.repository.OneTimeTokenRepository
import pitampoudel.komposeauth.user.service.OneTimeTokenService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.hours

@DataMongoTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Import(OneTimeTokenService::class)
class OneTimeTokenServiceTest {

    @Autowired private lateinit var service: OneTimeTokenService
    @Autowired private lateinit var repo: OneTimeTokenRepository

    @Test
    fun `createToken stores hashed token, findValidToken succeeds, consume makes it single-use`() {
        val userId = ObjectId()
        val raw = service.createToken(userId, OneTimeToken.Purpose.RESET_PASSWORD, ttl = 1.hours)

        // Repository should never store raw token
        val stored = repo.findAll().single()
        assertNotNull(stored.tokenHash)

        val validated = service.findValidToken(raw, OneTimeToken.Purpose.RESET_PASSWORD)
        assertEquals(stored.id, validated.id)

        val consumed = service.consume(raw, OneTimeToken.Purpose.RESET_PASSWORD)
        assertNotNull(consumed.consumedAt)

        assertThrows(IllegalStateException::class.java) {
            service.findValidToken(raw, OneTimeToken.Purpose.RESET_PASSWORD)
        }
    }

    @Test
    fun `findValidToken rejects purpose mismatch`() {
        val userId = ObjectId()
        val raw = service.createToken(userId, OneTimeToken.Purpose.VERIFY_EMAIL, ttl = 1.hours)

        assertThrows(IllegalArgumentException::class.java) {
            service.findValidToken(raw, OneTimeToken.Purpose.RESET_PASSWORD)
        }
    }

    companion object {
        @Container
        @JvmStatic
        var mongo: MongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:5.0"))

        @DynamicPropertySource
        @JvmStatic
        fun setProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") { mongo.connectionString }
            registry.add("spring.data.mongodb.database") { "test" }
        }
    }
}
