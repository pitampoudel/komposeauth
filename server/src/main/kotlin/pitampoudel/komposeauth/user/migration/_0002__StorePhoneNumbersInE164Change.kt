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
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.core.config.FlamingockConfig

/**
 * Stores every account's phone number the way it is now looked up: E.164, `+9779841234567`.
 *
 * Until December 2025 a number was saved in the international format, `+977 984-1234567`, or
 * sometimes as it was typed. Every lookup has since asked for E.164, so such an account is not found
 * by its number: its password sign-in fails, and an OTP sign-in makes a second, empty account beside
 * it. The runner written then for this (`PhoneMigrationRunner`) only ran under a `migration` profile,
 * so nothing shows it ever ran against the stored accounts.
 *
 * A number written without its country code is read in the configured default region, as a number
 * typed at sign-in is. One that can't be read as a phone number is left and logged, and so is one
 * whose E.164 form another account already holds: the older account, or the one already spelled that
 * way, keeps it.
 */
@Change(id = "store-phone-numbers-in-e164", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0002__StorePhoneNumbersInE164Change {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Safe to run again: a number already in E.164 is not touched. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        val region = mongoTemplate.getCollection(CONFIG)
            .find(Filters.eq("_id", AppConfig.SINGLETON_ID))
            .projection(Projections.include(REGION))
            .first()
            ?.getString(REGION)
            ?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
            ?: AppConfig.DEFAULT_PHONE_REGION

        val users = mongoTemplate.getCollection(USERS)
        val misspelled = users.find(Filters.type(PHONE, BsonType.STRING))
            .projection(Projections.include(PHONE))
            .sort(Sorts.ascending("_id"))
            .mapNotNull { user ->
                val stored = user.getString(PHONE)
                val e164 = parsePhoneNumber(region, stored)?.fullNumberInE164Format
                if (e164 == null) {
                    log.warn("Left account {} as it is: its phone number can't be read as one", user.getObjectId("_id"))
                    return@mapNotNull null
                }
                e164.takeIf { it != stored }?.let { user.getObjectId("_id") to it }
            }
            .toList()

        var converted = 0
        misspelled.forEach { (id, e164) ->
            val holder = users.find(Filters.eq(PHONE, e164)).projection(Projections.include("_id")).first()
            if (holder != null) {
                log.warn(
                    "Left account {} as it is: its phone number, in E.164, is account {}'s. Decide which " +
                        "of the two the person uses before changing either.",
                    id, holder.getObjectId("_id")
                )
                return@forEach
            }
            users.updateOne(Filters.eq("_id", id), Updates.set(PHONE, e164))
            converted++
        }
        if (converted > 0) log.info("Stored the phone number of {} accounts in E.164", converted)
    }

    /** Nothing to undo: the format each number had is not one anything looks up any more. */
    @Rollback
    fun rollback() = Unit

    private companion object {
        const val USERS = "users"
        const val PHONE = "phoneNumber"
        const val CONFIG = "config"
        const val REGION = "defaultPhoneRegion"
    }
}
