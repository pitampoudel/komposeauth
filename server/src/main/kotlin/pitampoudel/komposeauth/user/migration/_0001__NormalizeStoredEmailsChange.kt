package pitampoudel.komposeauth.user.migration

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Apply
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.Recovery
import io.flamingock.api.annotations.Rollback
import io.flamingock.api.annotations.TargetSystem
import org.bson.BsonType
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import pitampoudel.komposeauth.core.config.FlamingockConfig
import pitampoudel.komposeauth.core.utils.normalizedEmail

/**
 * Stores every account's email address the way it is now looked up: trimmed and lowercase.
 *
 * Sign-in finds an account by its address, by password and on every Google or Apple sign-in, and
 * the lookup now normalizes what it is given. The release before stored an address changed through
 * the profile or the email check as it was typed, so an account holding `Alice@Example.com` is no
 * longer found: its password stops working, and the next Google sign-in makes a second, empty
 * account beside it.
 *
 * Where two accounts' addresses differ only in case, one keeps its spelling and is logged: the other
 * already holds the normal one, or is older and is given it. Which of the two the person really uses
 * is not something a migration can tell.
 */
@Change(id = "normalize-stored-emails", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__NormalizeStoredEmailsChange {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Safe to run again: an address already in its normal form is not touched. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        val users = mongoTemplate.getCollection(USERS)
        val misspelled = users.find(Filters.type(EMAIL, BsonType.STRING))
            .projection(Projections.include(EMAIL))
            .sort(Sorts.ascending("_id"))
            .mapNotNull { user ->
                val stored = user.getString(EMAIL)
                stored.normalizedEmail().takeIf { it != stored }?.let { user.getObjectId("_id") to it }
            }
            .toList()

        var normalized = 0
        misspelled.forEach { (id, email) ->
            val holder = users.find(Filters.eq(EMAIL, email)).projection(Projections.include("_id")).first()
            if (holder != null) {
                log.warn(
                    "Left account {} as it is: its email address, normalized, is account {}'s. Decide which " +
                        "of the two the person uses before changing either.",
                    id, holder.getObjectId("_id")
                )
                return@forEach
            }
            users.updateOne(Filters.eq("_id", id), Updates.set(EMAIL, email))
            normalized++
        }
        if (normalized > 0) log.info("Normalized the email address of {} accounts", normalized)
    }

    /** Nothing to undo: the spelling each address had is not one anything looks up any more. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val USERS = "users"
        const val EMAIL = "email"
    }
}
