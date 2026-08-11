package pitampoudel.komposeauth.core.security.ratelimit

import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * One fixed window of one rate-limit key.
 *
 * The id is `<key>|<window>`, so a new window is a new document and the old one ages out on its own
 * — no sweeping, and nothing to reset when an instance restarts.
 */
@Document(collection = "rate_limits")
@TypeAlias("rate_limit")
data class RateLimitWindow(
    @Id
    val id: String,
    val count: Long = 0,
    /** Mongo's TTL monitor removes the document once this passes, so the collection self-prunes. */
    @Indexed(expireAfter = "0s")
    val expiresAt: Instant
)

/**
 * Counters shared by every instance.
 *
 * This is what makes the limits mean anything when the app is horizontally scaled or running
 * serverless: the tally lives next to the data rather than in one process's heap, so it survives
 * cold starts and is not multiplied by the instance count.
 */
@Service
class MongoRateLimitStore(
    private val mongoTemplate: MongoTemplate
) : RateLimitStore {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun incrementAndCount(bucketKey: String, expiresAt: Instant): Long {
        return try {
            // A single round trip that both counts and returns the total. `$inc` is atomic and the
            // upsert races safely: concurrent callers on different instances each get their own
            // number back, so no two of them can both believe they were the Nth request.
            val updated = mongoTemplate.findAndModify(
                Query(Criteria.where("_id").`is`(bucketKey)),
                Update().inc("count", 1).setOnInsert("expiresAt", expiresAt),
                FindAndModifyOptions().upsert(true).returnNew(true),
                RateLimitWindow::class.java
            )
            updated?.count ?: 1
        } catch (ex: Exception) {
            // Fail open. Mongo is this app's primary store, so if it is unreachable the request is
            // going to fail on its own a moment later; refusing traffic here would only convert a
            // database outage into a second, more confusing one.
            log.warn("Rate limit counter unavailable for '{}', allowing request", bucketKey, ex)
            0
        }
    }
}
