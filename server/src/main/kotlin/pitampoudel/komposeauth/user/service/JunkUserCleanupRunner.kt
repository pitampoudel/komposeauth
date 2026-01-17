package pitampoudel.komposeauth.user.service

import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import pitampoudel.komposeauth.user.entity.User

@Component
@ConditionalOnProperty(prefix = "cleanup.junk-users", name = ["enabled"], havingValue = "true")
class JunkUserCleanupRunner(
    private val mongoTemplate: MongoTemplate
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)
    private val trustedEmailDomains = listOf("gmail.com", "outlook.com", "yahoo.com", "icloud.com")
    private val trustedPhonePrefixes = listOf("+977")

    override fun run(args: ApplicationArguments) {
        val emailPattern = buildEmailPattern(trustedEmailDomains)
        val phonePattern = buildPhonePattern(trustedPhonePrefixes)
        log.info("Using email pattern: {}", emailPattern)
        log.info("Using phone pattern: {}", phonePattern)

        val criteria = Criteria().andOperator(
            Criteria.where("emailVerified").ne(true),
            Criteria.where("phoneNumberVerified").ne(true),
            Criteria.where("email").not().regex(emailPattern, "i"),
            Criteria.where("phoneNumber").not().regex(phonePattern)
        )

        val ids = mutableListOf<ObjectId>()

        mongoTemplate.stream(Query(criteria), User::class.java).use { stream ->
            val iterator = stream.iterator()
            while (iterator.hasNext()) {
                val doc = iterator.next()
                ids.add(doc.id)
            }
        }

        if (ids.isEmpty()) {
            log.info("Junk cleanup: no candidates found")
            return
        }

        log.info("Junk cleanup: candidates={}", ids.size)

        val deleteResult = mongoTemplate.remove(Query(Criteria("_id").`in`(ids)), User::class.java)
        log.info(
            "Junk cleanup: deleted={} acknowledged={}",
            deleteResult.deletedCount,
            deleteResult.wasAcknowledged()
        )

        log.info("Junk cleanup finished; deleted {} users", ids.size)
    }

    private fun buildEmailPattern(domains: List<String>): String {
        if (domains.isEmpty()) return "^[^@]+@.+$"
        val escaped = domains.mapNotNull { it.trim().lowercase().takeIf(String::isNotEmpty)?.let(::escapeRegex) }
        if (escaped.isEmpty()) return "^[^@]+@.+$"
        return "^[^@]+@(${escaped.joinToString("|")})$"
    }

    private fun buildPhonePattern(prefixes: List<String>): String {
        if (prefixes.isEmpty()) return "^.*$"
        val escaped = prefixes.mapNotNull { it.trim().takeIf(String::isNotEmpty)?.let(::escapeRegex) }
        if (escaped.isEmpty()) return "^.*$"
        return "^(${escaped.joinToString("|")})"
    }

    private fun escapeRegex(value: String): String =
        value.replace(Regex("""[-/\\^$*+?.()|{}]""")) { match -> "\\" + match.value }
}
