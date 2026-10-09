package pitampoudel.komposeauth.otp.service

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import pitampoudel.komposeauth.otp.entity.Otp
import pitampoudel.komposeauth.otp.repository.OtpRepository
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

/** The codes we text or email ourselves, for every channel that has no provider checking them for us. */
@Service
class OtpCodes(
    private val otpRepository: OtpRepository,
    private val mongoTemplate: MongoTemplate
) {

    /** Files a fresh code for [receiver] and returns it for the caller to deliver. */
    fun issue(receiver: String): String {
        val latest = otpRepository.findByReceiverOrderByCreatedAtDesc(receiver).firstOrNull()
        if (latest != null && latest.createdAt.isAfter(Instant.now().minus(RESEND_COOLDOWN))) {
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "OTP already sent. Please wait ${RESEND_COOLDOWN.seconds} seconds before requesting again."
            )
        }
        return OtpGenerator.next().also { otpRepository.save(Otp(receiver = receiver, otp = it)) }
    }

    /**
     * Whether [code] is the latest live code sent to [receiver]. A match uses up every code it has.
     *
     * A code signs a user in on its own, and the per-address request limits do not bound guesses
     * spread over many addresses, so each code also allows [MAX_ATTEMPTS] checks in all. The attempt
     * is taken in one atomic update before comparing, so parallel guesses cannot share one.
     */
    fun redeem(receiver: String, code: String): Boolean {
        val latest = otpRepository.findByReceiverOrderByCreatedAtDesc(receiver).firstOrNull() ?: return false
        if (latest.isExpired()) return false
        mongoTemplate.findAndModify(
            Query(Criteria.where("_id").`is`(latest.id).and("attempts").not().gte(MAX_ATTEMPTS)),
            Update().inc("attempts", 1),
            Otp::class.java
        ) ?: return false
        if (!MessageDigest.isEqual(latest.otp.toByteArray(), code.toByteArray())) return false
        otpRepository.deleteByReceiver(receiver)
        return true
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        val RESEND_COOLDOWN: Duration = Duration.ofSeconds(60)
    }
}
