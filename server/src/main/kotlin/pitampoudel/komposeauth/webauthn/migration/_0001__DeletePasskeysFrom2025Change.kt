package pitampoudel.komposeauth.webauthn.migration

import com.mongodb.client.model.Filters
import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Apply
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.Recovery
import io.flamingock.api.annotations.Rollback
import io.flamingock.api.annotations.TargetSystem
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import pitampoudel.komposeauth.core.config.FlamingockConfig
import pitampoudel.komposeauth.webauthn.entity.PublicKeyCredential
import pitampoudel.komposeauth.webauthn.entity.PublicKeyUser

/**
 * Deletes the passkeys registered before January 2026, which nothing has been able to read since.
 *
 * Their user was keyed by its handle as text with no account, and the passkey kept `lastUsed` and no
 * account. Such a passkey can't sign in, and its owner can't register a new one, because reading the
 * old user fails first. Once it is gone they can.
 */
@Change(id = "delete-passkeys-from-2025", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__DeletePasskeysFrom2025Change {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Safe to run again: a passkey or user that reads is never matched. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        val users = deleteUnreadable(mongoTemplate, USERS, PublicKeyUser::class.java)
        val credentials = deleteUnreadable(mongoTemplate, CREDENTIALS, PublicKeyCredential::class.java)
        if (users + credentials > 0) log.warn("Deleted {} passkeys and {} passkey users from 2025", credentials, users)
    }

    private fun deleteUnreadable(mongoTemplate: MongoTemplate, collectionName: String, type: Class<*>): Int {
        val collection = mongoTemplate.getCollection(collectionName)
        val unreadable = collection.find().filter {
            runCatching { mongoTemplate.converter.read(type, it) }.isFailure
        }.map { it["_id"] }.toList()
        if (unreadable.isNotEmpty()) collection.deleteMany(Filters.`in`("_id", unreadable))
        return unreadable.size
    }

    /** Nothing to undo: no release since January 2026 could read these. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val USERS = "public_key_users"
        const val CREDENTIALS = "public_key_credentials"
    }
}
