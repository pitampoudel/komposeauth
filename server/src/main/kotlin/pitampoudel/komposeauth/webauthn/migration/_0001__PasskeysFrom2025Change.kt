package pitampoudel.komposeauth.webauthn.migration

import com.mongodb.MongoWriteException
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates
import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Apply
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.Recovery
import io.flamingock.api.annotations.Rollback
import io.flamingock.api.annotations.TargetSystem
import org.bson.Document
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.security.web.webauthn.api.Bytes
import pitampoudel.komposeauth.core.config.FlamingockConfig
import pitampoudel.komposeauth.core.utils.normalizedEmail
import pitampoudel.komposeauth.webauthn.entity.PublicKeyUser
import java.util.Date

/**
 * Lets the passkeys registered before January 2026 sign their owners in again.
 *
 * Until then a passkey's user was keyed by its handle, written as text, with no account named, and
 * a passkey kept `lastUsed` and no account either. Since January both are read with the account they
 * belong to and nothing converted the ones already stored, so such a passkey can't sign in and its
 * owner can't register a new one: reading their old user fails before anything else is tried.
 *
 * Each old user is set aside in `public_key_users_legacy` and put back with its handle, which the
 * passkey on the person's device still sends, and the account its name finds. The name is what the
 * account was signed in as, its id or its email, and the first releases kept it as `email`. Then each
 * old passkey is given the account of the user it belongs to. A user whose name finds no account,
 * or whose account has registered a passkey since, stays in the legacy collection for a person to
 * look at, and a passkey left without a user moves to `public_key_credentials_legacy`.
 */
@Change(id = "passkeys-from-2025", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__PasskeysFrom2025Change {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Safe to run again: an old user is set aside before it is removed, and a converted one is not matched. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        restoreUsers(mongoTemplate)
        restoreCredentials(mongoTemplate)
    }

    private fun restoreUsers(mongoTemplate: MongoTemplate) {
        val users = mongoTemplate.getCollection(USERS)
        val legacy = mongoTemplate.getCollection(USERS_LEGACY)
        val accounts = mongoTemplate.getCollection(ACCOUNTS)

        users.find(Filters.exists(USER_HANDLE, false)).toList().forEach { old ->
            legacy.replaceOne(Filters.eq("_id", old["_id"]), old, ReplaceOptions().upsert(true))
            users.deleteOne(Filters.eq("_id", old["_id"]))
        }

        var restored = 0
        val kept = mutableListOf<Any>()
        legacy.find().toList().forEach { old ->
            val handle = (old["_id"] as? String)?.let { runCatching { Bytes.fromBase64(it) }.getOrNull() }
            val name = old.getString("name") ?: old.getString("email")
            val account = name?.let { accountNamed(accounts, it) }
            val restorable = handle != null && name != null && account != null && (
                users.find(Filters.eq(USER_HANDLE, handleOf(mongoTemplate, handle))).first() != null ||
                    insert(mongoTemplate, users, old, handle, name, account)
                )
            if (restorable) {
                legacy.deleteOne(Filters.eq("_id", old["_id"]))
                restored++
            } else {
                kept += old["_id"]!!
            }
        }
        if (restored > 0) log.info("Restored {} passkey users from before January 2026", restored)
        if (kept.isNotEmpty()) {
            log.warn("{} passkey users from before January 2026 stay in {} for a person to look at: {}", kept.size, USERS_LEGACY, kept)
        }
    }

    private fun insert(
        mongoTemplate: MongoTemplate,
        users: MongoCollection<Document>,
        old: Document,
        handle: Bytes,
        name: String,
        account: ObjectId
    ): Boolean {
        val user = PublicKeyUser(
            id = ObjectId(),
            userId = account,
            userHandle = handle,
            name = name,
            displayName = old.getString("displayName"),
            createdAt = (old.getDate("createdAt") ?: Date()).toInstant(),
            updatedAt = (old.getDate("updatedAt") ?: Date()).toInstant()
        )
        val document = Document().also { mongoTemplate.converter.write(user, it) }
        return try {
            users.insertOne(document)
            true
        } catch (_: MongoWriteException) {
            false
        }
    }

    private fun restoreCredentials(mongoTemplate: MongoTemplate) {
        val users = mongoTemplate.getCollection(USERS)
        val credentials = mongoTemplate.getCollection(CREDENTIALS)
        val legacy = mongoTemplate.getCollection(CREDENTIALS_LEGACY)

        var restored = 0
        val kept = mutableListOf<Any>()
        credentials.find(Filters.exists(ACCOUNT, false)).toList().forEach { old ->
            val account = users.find(Filters.eq(USER_HANDLE, old["publicKeyUserId"])).first()?.getObjectId(ACCOUNT)
            if (account == null) {
                legacy.replaceOne(Filters.eq("_id", old["_id"]), old, ReplaceOptions().upsert(true))
                credentials.deleteOne(Filters.eq("_id", old["_id"]))
                kept += old["_id"]!!
                return@forEach
            }
            val lastUsed = old.getDate("lastUsed") ?: old.getDate("createdAt") ?: Date()
            credentials.updateOne(
                Filters.eq("_id", old["_id"]),
                Updates.combine(Updates.set(ACCOUNT, account), Updates.set("lastUsedAt", lastUsed), Updates.unset("lastUsed"))
            )
            restored++
        }
        if (restored > 0) log.info("Restored {} passkeys from before January 2026", restored)
        if (kept.isNotEmpty()) {
            log.warn("{} passkeys from before January 2026 belong to no user and moved to {}", kept.size, CREDENTIALS_LEGACY)
        }
    }

    /** The handle as a passkey and a converted user store it. */
    private fun handleOf(mongoTemplate: MongoTemplate, handle: Bytes): Any =
        mongoTemplate.converter.convertToMongoType(handle)!!

    /** The account a name signed in as, found the way sign-in found it then: by id, email, or phone. */
    private fun accountNamed(accounts: MongoCollection<Document>, name: String): ObjectId? {
        val byId = if (ObjectId.isValid(name)) Filters.eq("_id", ObjectId(name)) else null
        return listOfNotNull(byId, Filters.eq("email", name.normalizedEmail()), Filters.eq("phoneNumber", name))
            .firstNotNullOfOrNull { accounts.find(it).first()?.getObjectId("_id") }
    }

    /** Nothing to undo: every user and passkey set aside is kept as it was stored. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val USERS = "public_key_users"
        const val USERS_LEGACY = "public_key_users_legacy"
        const val CREDENTIALS = "public_key_credentials"
        const val CREDENTIALS_LEGACY = "public_key_credentials_legacy"
        const val ACCOUNTS = "users"
        const val USER_HANDLE = "userHandle"
        const val ACCOUNT = "userId"
    }
}
