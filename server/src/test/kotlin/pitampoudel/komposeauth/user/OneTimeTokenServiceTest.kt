package pitampoudel.komposeauth.user

import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.hours

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(OneTimeTokenService::class, TestConfig::class)
class OneTimeTokenServiceTest {

    @Autowired
    private lateinit var service: OneTimeTokenService


    @Test
    fun `createToken stores hashed token, findValidToken succeeds, consume makes it single-use`() {
        val userId = ObjectId()
        val raw = service.createToken(userId, OneTimeToken.Purpose.RESET_PASSWORD, ttl = 1.hours)

        // Repository should never store raw token
        val stored = service.findValidToken(raw, OneTimeToken.Purpose.RESET_PASSWORD)
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

}
