package pitampoudel.komposeauth.webauthn.entity

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Options handed to a client that has not yet answered them.
 *
 * Keyed by the challenge itself: every answer a client sends back carries the challenge inside its
 * signed `clientDataJSON`, so it is found again without a cookie, a session or any id of our own —
 * which is what lets a native app, holding no cookie jar, finish what it started.
 */
@Document(collection = "webauthn_challenges")
@TypeAlias("webauthn_challenge")
data class WebAuthnChallenge(
    /** SHA-256 of the challenge's bytes, base64url. */
    @Id
    val id: String,
    val ceremony: Ceremony,
    /** Whom a registration's options were made for; nobody else may finish it. Null for sign-in. */
    val userId: ObjectId? = null,
    /** The options exactly as they were sent, in the WebAuthn JSON shape. */
    val optionsJson: String,
    @Indexed(expireAfter = "0s")
    val expiresAt: Instant,
    val createdAt: Instant = Instant.now()
) {
    enum class Ceremony {
        AUTHENTICATION,
        REGISTRATION
    }
}
