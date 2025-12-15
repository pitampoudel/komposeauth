package pitampoudel.komposeauth.migration

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.komposeauth.user.repository.UserRepository

@Component
@Profile("migration")
class PhoneMigrationRunner(
    private val repo: UserRepository
) : ApplicationRunner {

    fun toE164(
        phone: String?
    ): String? {
        return try {
            return if (!phone.isNullOrBlank())
                parsePhoneNumber(null, phone)?.fullNumberInE164Format
            else null
        } catch (e: Exception) {
            null
        }
    }

    override fun run(args: ApplicationArguments) {
        val users = repo.findAllHavingPhoneNumber()

        var updated = 0
        var skipped = 0

        users.forEach { user ->
            val e164 = toE164(user.phoneNumber)

            if (e164 == null) {
                skipped++
                return@forEach
            }

            repo.save(user.copy(phoneNumber = e164))
            updated++
        }

        println("Phone migration complete → updated=$updated skipped=$skipped")
    }
}
