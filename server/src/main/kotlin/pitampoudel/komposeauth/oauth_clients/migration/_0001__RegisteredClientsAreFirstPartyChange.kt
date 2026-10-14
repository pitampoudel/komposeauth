package pitampoudel.komposeauth.oauth_clients.migration

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Apply
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.Recovery
import io.flamingock.api.annotations.Rollback
import io.flamingock.api.annotations.TargetSystem
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import pitampoudel.komposeauth.core.config.FlamingockConfig

/**
 * Stops the clients registered here before February 2026 reading as somebody else's app.
 *
 * Until then every client the console registered was saved with `requireAuthorizationConsent: true`,
 * and nothing offered a way to change it. A client that asks the user's consent is now read as a
 * third party's: its access tokens carry no roles, this server's own API refuses them, and it is told
 * only what the user consented to. So an app registered then would lose its users' roles and its
 * calls to this server on the first start of this release. Every client registered since was saved
 * without consent, and these are given the same.
 *
 * `swagger-ui` is deleted instead: the server registered it for itself, for a Swagger sign-in that
 * was removed five days later, and nothing has used it since.
 */
@Change(id = "registered-clients-are-first-party", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__RegisteredClientsAreFirstPartyChange {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Safe to run again: a client that does not ask for consent is not touched. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        val clients = mongoTemplate.getCollection(CLIENTS)
        clients.deleteOne(Filters.eq("_id", SWAGGER_UI))
        val changed = clients.updateMany(Filters.eq(CONSENT, true), Updates.set(CONSENT, false)).modifiedCount
        if (changed > 0) log.info("{} clients registered before February 2026 no longer ask for consent", changed)
    }

    /** Nothing to undo: no client registered here was ever meant to ask for consent. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val CLIENTS = "oauth2_clients"
        const val CONSENT = "requireAuthorizationConsent"
        const val SWAGGER_UI = "swagger-ui"
    }
}
