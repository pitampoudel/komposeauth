package pitampoudel.komposeauth.webauthn.service

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.webauthn.entity.WebAuthnChallenge
import pitampoudel.komposeauth.webauthn.entity.WebAuthnChallenge.Ceremony
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Base64

/**
 * Keeps the options of a ceremony in progress until the client answers them, for no longer than
 * the options' own timeout, and gives each one out once.
 */
@Service
class WebAuthnChallengeStore(
    private val mongoTemplate: MongoTemplate,
    private val webAuthnJson: WebAuthnJson
) {
    fun save(options: PublicKeyCredentialRequestOptions) {
        insert(options.challenge, Ceremony.AUTHENTICATION, null, webAuthnJson.write(options), options.timeout)
    }

    fun save(userId: ObjectId, options: PublicKeyCredentialCreationOptions) {
        insert(options.challenge, Ceremony.REGISTRATION, userId, webAuthnJson.write(options), options.timeout)
    }

    /** The sign-in options issued with [challenge], removed so they cannot be answered twice. */
    fun takeRequestOptions(challenge: Bytes): PublicKeyCredentialRequestOptions? =
        take(challenge, Ceremony.AUTHENTICATION, null)?.let { webAuthnJson.readRequestOptions(it.optionsJson) }

    /** As [takeRequestOptions], for registration options issued to [userId] and nobody else. */
    fun takeCreationOptions(challenge: Bytes, userId: ObjectId): PublicKeyCredentialCreationOptions? =
        take(challenge, Ceremony.REGISTRATION, userId)?.let { webAuthnJson.readCreationOptions(it.optionsJson) }

    private fun insert(challenge: Bytes, ceremony: Ceremony, userId: ObjectId?, optionsJson: String, timeout: Duration?) {
        val now = Instant.now()
        mongoTemplate.insert(
            WebAuthnChallenge(
                id = key(challenge),
                ceremony = ceremony,
                userId = userId,
                optionsJson = optionsJson,
                expiresAt = now + (timeout ?: DEFAULT_LIFETIME),
                createdAt = now
            )
        )
    }

    /**
     * Find-and-remove in one step, so that of two answers arriving together only one gets the
     * options. Expiry is in the query because the TTL monitor only sweeps once a minute.
     */
    private fun take(challenge: Bytes, ceremony: Ceremony, userId: ObjectId?): WebAuthnChallenge? {
        val criteria = Criteria.where("_id").`is`(key(challenge))
            .and("ceremony").`is`(ceremony)
            .and("expiresAt").gt(Instant.now())
        if (userId != null) criteria.and("userId").`is`(userId)
        return mongoTemplate.findAndRemove(Query(criteria), WebAuthnChallenge::class.java)
    }

    private fun key(challenge: Bytes): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(challenge.bytes))

    private companion object {
        val DEFAULT_LIFETIME: Duration = Duration.ofMinutes(5)
    }
}
