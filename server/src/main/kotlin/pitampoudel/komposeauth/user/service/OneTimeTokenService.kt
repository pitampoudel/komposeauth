package pitampoudel.komposeauth.user.service

import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.AppProperties
import pitampoudel.komposeauth.data.ApiEndpoints
import pitampoudel.komposeauth.user.entity.OneTimeToken
import pitampoudel.komposeauth.user.repository.OneTimeTokenRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Service
class OneTimeTokenService(
    private val repo: OneTimeTokenRepository,
    private val appProperties: AppProperties,
) {
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun generateRefreshToken(userId: ObjectId, ttl: Duration = 30.days): String {
        return createToken(userId, OneTimeToken.Purpose.REFRESH_TOKEN, ttl)

    }

    fun generateEmailVerificationLink(userId: ObjectId, ttl: Duration = 24.hours): String {
        val token = createToken(userId, OneTimeToken.Purpose.VERIFY_EMAIL, ttl)
        return "${appProperties.selfBaseUrl}/${ApiEndpoints.VERIFY_EMAIL}?token=$token"
    }

    fun generateResetPasswordLink(userId: ObjectId, ttl: Duration = 24.hours): String {
        val token = createToken(userId, OneTimeToken.Purpose.RESET_PASSWORD, ttl)
        return "${appProperties.selfBaseUrl}/${ApiEndpoints.RESET_PASSWORD}?token=$token"
    }

    fun createToken(userId: ObjectId, purpose: OneTimeToken.Purpose, ttl: Duration): String {
        fun newRandomToken(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return encoder.encodeToString(bytes)
        }

        val raw = newRandomToken()
        val now = Instant.now()
        val entity = OneTimeToken(
            userId = userId,
            purpose = purpose,
            tokenHash = hash(raw),
            expiresAt = now.plusSeconds(ttl.inWholeSeconds)
        )
        repo.save(entity)
        return raw
    }

    fun verify(rawToken: String, purpose: OneTimeToken.Purpose): OneTimeToken {
        val token = repo.findByTokenHashAndPurpose(hash(rawToken), purpose)
            ?: throw IllegalArgumentException("Invalid or unknown token")
        if (token.isConsumed()) throw IllegalStateException("Token already used")
        if (token.isExpired()) throw IllegalStateException("Token expired")
        return token
    }

    private fun hash(raw: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
        return encoder.encodeToString(digest)
    }

    fun consume(rawToken: String, purpose: OneTimeToken.Purpose): OneTimeToken {
        val token = verify(rawToken, purpose)
        val consumed = token.copy(consumedAt = Instant.now())
        return repo.save(consumed)
    }
}
