package pitampoudel.komposeauth.authorization.migration

import com.mongodb.client.model.Filters
import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Apply
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.Recovery
import io.flamingock.api.annotations.Rollback
import io.flamingock.api.annotations.TargetSystem
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import pitampoudel.komposeauth.authorization.OAuth2AuthorizationDocument
import pitampoudel.komposeauth.core.config.FlamingockConfig

/**
 * Deletes the authorizations that hold no token and no expiry.
 *
 * One waiting for the user's consent holds only its state. The release before saved it with no
 * `expiresAt`, which the TTL index on that field never removes, so every consent screen nobody
 * answered stayed in the collection for good. It is now saved to go ten minutes later
 * ([OAuth2AuthorizationDocument]); the ones already there go now. Somebody on a consent screen
 * while this runs signs in again.
 */
@Change(id = "delete-abandoned-consents", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__DeleteAbandonedConsentsChange {
    private val log = LoggerFactory.getLogger(javaClass)

    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        val deleted = mongoTemplate.getCollection(AUTHORIZATIONS).deleteMany(
            Filters.and(
                Filters.eq("expiresAt", null),
                Filters.eq("authorizationCodeValue", null),
                Filters.eq("accessTokenValue", null),
                Filters.eq("refreshTokenValue", null)
            )
        ).deletedCount
        if (deleted > 0) log.info("Deleted {} authorizations left waiting for consent", deleted)
    }

    /** Nothing to undo: an authorization that holds no token is not one anybody can still use. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val AUTHORIZATIONS = "oauth2_authorizations"
    }
}
