package pitampoudel.komposeauth.authorization

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Refresh tokens are rotated: a refresh returns a new one and retires the one it was made with.
 *
 * A server-rendered client refreshes from one cookie several times when a page load crosses the
 * access token's expiry, since each concurrent request refreshes on its own, and a token that answers
 * only once would sign the person out on the second. So a retired token stays good for
 * [ROTATION_GRACE], and is answered with the token that replaced it rather than with yet another, so
 * every one of those requests ends up holding the same token. Presented after that it is a replay —
 * somebody else has a copy — and the authorization ends.
 */
@Service
class MongoOAuth2AuthorizationService(
    private val repository: OAuth2AuthorizationDocumentRepository,
    private val registeredClientRepository: RegisteredClientRepository,
    private val objectMapper: ObjectMapper,
    private val mongoTemplate: MongoTemplate,
) : OAuth2AuthorizationService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(authorization: OAuth2Authorization) {
        val doc = toOAuth2AuthorizationDocument(authorization, objectMapper)
        // The rotation is recorded by rotateRefreshToken and is not part of the authorization, so a
        // save keeps it.
        val stored = repository.findById(doc.id).orElse(null)
        repository.save(
            doc.copy(
                previousRefreshTokenValue = stored?.previousRefreshTokenValue,
                refreshTokenRotatedAt = stored?.refreshTokenRotatedAt
            )
        )
    }

    override fun remove(authorization: OAuth2Authorization) {
        repository.deleteById(authorization.id)
    }

    override fun findById(id: String): OAuth2Authorization? =
        // Generic lookups can feed token generation, so keep claims serializer-friendly.
        repository.findById(id).orElse(null)?.toAuthorization(OidcClaimTypeMode.SERIALIZATION)

    override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? {
        val doc = when {
            tokenType == null -> repository.findByState(token)
                ?: repository.findByAuthorizationCodeValue(token)
                ?: repository.findByAccessTokenValue(token)
                ?: repository.findByRefreshTokenValue(token)
                ?: repository.findByOidcIdTokenValue(token)
            tokenType == OAuth2TokenType.ACCESS_TOKEN -> repository.findByAccessTokenValue(token)
            tokenType == OAuth2TokenType.REFRESH_TOKEN -> repository.findByRefreshTokenValue(token)
                ?: retiredWithinGrace(token)
            // The consent form posts the state back, for clients that ask for the user's consent
            tokenType.value == OAuth2ParameterNames.STATE -> repository.findByState(token)
            tokenType.value == "code" -> repository.findByAuthorizationCodeValue(token)
            tokenType.value == "id_token" -> repository.findByOidcIdTokenValue(token)
            else -> null
        }
        return doc?.toAuthorization(
            // UserInfo reads access-token claims through Spring OIDC accessors.
            if (tokenType == OAuth2TokenType.ACCESS_TOKEN) {
                OidcClaimTypeMode.SPRING_ACCESSORS
            } else {
                OidcClaimTypeMode.SERIALIZATION
            }
        )
    }

    /**
     * The authorization whose refresh token replaced [token] less than [ROTATION_GRACE] ago. Past
     * that, [token] is a replay and the authorization is ended instead.
     *
     * Not part of [findByToken] without a type, which revocation and introspection use: the
     * authorization no longer holds [token], and both go on to ask it for the token they looked up.
     */
    fun findByRetiredRefreshToken(token: String): OAuth2Authorization? =
        retiredWithinGrace(token)?.toAuthorization(OidcClaimTypeMode.SERIALIZATION)

    private fun OAuth2AuthorizationDocument.toAuthorization(claimTypeMode: OidcClaimTypeMode): OAuth2Authorization? =
        fromOAuth2AuthorizationDocument(
            doc = this,
            registeredClient = registeredClientRepository.findById(registeredClientId) ?: return null,
            objectMapper = objectMapper,
            claimTypeMode = claimTypeMode
        )

    /**
     * Replaces [presented], the authorization's refresh token, with [replacement], in one step so
     * that only one refresh can. Any other refresh made with [presented] within [ROTATION_GRACE]
     * gets the token that won instead. Null when [presented] is neither the authorization's refresh
     * token nor the one it just replaced.
     */
    fun rotateRefreshToken(
        authorizationId: String,
        presented: String,
        replacement: OAuth2RefreshToken
    ): OAuth2RefreshToken? {
        val now = Instant.now()
        val replaced = mongoTemplate.findAndModify(
            Query(Criteria.where("id").`is`(authorizationId).and("refreshTokenValue").`is`(presented)),
            Update()
                .set("refreshTokenValue", replacement.tokenValue)
                .set("refreshTokenIssuedAt", replacement.issuedAt)
                .set("refreshTokenExpiresAt", replacement.expiresAt)
                .set("expiresAt", replacement.expiresAt)
                .set("previousRefreshTokenValue", presented)
                .set("refreshTokenRotatedAt", now),
            OAuth2AuthorizationDocument::class.java
        )
        if (replaced != null) return replacement

        val doc = repository.findById(authorizationId).orElse(null) ?: return null
        val current = doc.refreshTokenValue ?: return null
        if (doc.previousRefreshTokenValue != presented || !doc.rotatedWithinGrace(now)) return null
        return OAuth2RefreshToken(current, doc.refreshTokenIssuedAt, doc.refreshTokenExpiresAt)
    }

    private fun retiredWithinGrace(token: String): OAuth2AuthorizationDocument? {
        val doc = repository.findByPreviousRefreshTokenValue(token) ?: return null
        if (doc.rotatedWithinGrace(Instant.now())) return doc
        log.warn(
            "A refresh token of client {} was used again {} after it was replaced; ending authorization {}",
            doc.registeredClientId, doc.refreshTokenRotatedAt?.let { Duration.between(it, Instant.now()) }, doc.id
        )
        repository.deleteById(doc.id)
        return null
    }

    private fun OAuth2AuthorizationDocument.rotatedWithinGrace(now: Instant): Boolean =
        refreshTokenRotatedAt?.let { !now.isAfter(it.plus(ROTATION_GRACE)) } == true

    companion object {
        /** How long the refresh token a rotation retires is still answered, for concurrent refreshes. */
        val ROTATION_GRACE: Duration = Duration.ofMinutes(2)
    }
}
