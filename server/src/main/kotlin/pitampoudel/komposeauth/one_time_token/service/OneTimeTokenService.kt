package pitampoudel.komposeauth.one_time_token.service

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
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
    private val repo: OneTimeTokenRepository,
    private val mongoTemplate: MongoTemplate
) {
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    private companion object {
        const val INVALID_TOKEN_MESSAGE = "This link is invalid or has expired. Request a new one."
    }

    fun generateRefreshToken(userId: ObjectId, ttl: Duration = 30.days): String {
        return createToken(userId, OneTimeToken.Purpose.REFRESH_TOKEN, ttl)

    }

    /**
     * @param email the address the link is being sent to, recorded on the token so that clicking it
     * verifies *that* address rather than whatever the account happens to hold on arrival.
     */
    fun generateEmailVerificationLink(
        userId: ObjectId, ttl: Duration = 24.hours, baseUrl: String, email: String
    ): String {
        val token = createToken(
            userId = userId,
            purpose = OneTimeToken.Purpose.VERIFY_EMAIL,
            ttl = ttl,
            subject = email.lowercase()
        )
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

    fun createToken(
        userId: ObjectId,
        purpose: OneTimeToken.Purpose,
        ttl: Duration,
        subject: String? = null
    ): String {
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
            expiresAt = now.plusSeconds(ttl.inWholeSeconds),
            subject = subject
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

    /**
     * Spends a token, and does it in one step so that exactly one caller can.
     *
     * The read-then-write this replaces did not make a token single-use, only single-use *eventually*:
     * two requests arriving together both read `consumedAt == null`, both decided the token was good,
     * and both wrote their own timestamp over the other's. What each one then did with that answer is
     * the part that matters — a `REFRESH_TOKEN` is redeemed here for a fresh access token and a fresh
     * refresh token, so a leaked one could be redeemed as many times as it was presented at once, and
     * a reset link could set the password twice from two different submissions.
     *
     * `findAndModify` moves the "is it still unspent?" test into the same document update that spends
     * it, which the server performs atomically per document. The loser of the race matches nothing —
     * `consumedAt` is no longer null by the time its query runs — and gets the same 400 as any other
     * spent link, which is what it should have got all along.
     *
     * Expiry is folded into the query for the same reason. Comparing `expiresAt` in Kotlin against a
     * document that Mongo may drop under its TTL index mid-flight leaves a window; comparing it in
     * the query does not.
     */
    fun consume(rawToken: String, purpose: OneTimeToken.Purpose): OneTimeToken {
        val now = Instant.now()
        val query = Query(
            Criteria.where("tokenHash").`is`(hash(rawToken))
                .and("purpose").`is`(purpose)
                .and("consumedAt").`is`(null)
                .and("expiresAt").gt(now)
        )
        return mongoTemplate.findAndModify(
            query,
            Update().set("consumedAt", now),
            FindAndModifyOptions.options().returnNew(true),
            OneTimeToken::class.java
        ) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_TOKEN_MESSAGE)
    }
}