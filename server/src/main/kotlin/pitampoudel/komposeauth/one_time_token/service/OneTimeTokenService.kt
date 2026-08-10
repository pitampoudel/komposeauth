package pitampoudel.komposeauth.one_time_token.service

import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.repository.OneTimeTokenRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Service
class OneTimeTokenService(
    private val repo: OneTimeTokenRepository
) {
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    private companion object {
        const val INVALID_TOKEN_MESSAGE = "This link is invalid or has expired. Request a new one."
    }

    fun generateRefreshToken(userId: ObjectId, ttl: Duration = 30.days): String {
        return createToken(userId, OneTimeToken.Purpose.REFRESH_TOKEN, ttl)

    }

    fun generateEmailVerificationLink(
        userId: ObjectId, ttl: Duration = 24.hours, baseUrl: String
    ): String {
        val token = createToken(userId, OneTimeToken.Purpose.VERIFY_EMAIL, ttl)
        return "${baseUrl}/${ApiEndpoints.VERIFY_EMAIL}?token=$token"
    }

    fun generateResetPasswordLink(
        userId: ObjectId,
        ttl: Duration = 24.hours,
        baseUrl: String
    ): String {
        val token = createToken(userId, OneTimeToken.Purpose.RESET_PASSWORD, ttl)
        return "$baseUrl/${ApiEndpoints.RESET_PASSWORD}?token=$token"
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

    /**
     * A bad token is a client error, so these surface as 400 rather than falling through to the
     * catch-all handler as a 500 (and paging whoever watches Sentry every time a reset link is
     * clicked twice). All three cases share one message so the response can't be used to tell an
     * unknown token from a spent one.
     */
    fun findValidToken(rawToken: String, purpose: OneTimeToken.Purpose): OneTimeToken {
        val token = repo.findByTokenHashAndPurpose(hash(rawToken), purpose)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_TOKEN_MESSAGE)
        if (token.isConsumed() || token.isExpired()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_TOKEN_MESSAGE)
        }
        return token
    }

    private fun hash(raw: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
        return encoder.encodeToString(digest)
    }

    fun consume(rawToken: String, purpose: OneTimeToken.Purpose): OneTimeToken {
        val token = findValidToken(rawToken, purpose)
        val consumed = token.copy(consumedAt = Instant.now())
        return repo.save(consumed)
    }
}