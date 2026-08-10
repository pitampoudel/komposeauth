package pitampoudel.komposeauth.core.security.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Tuning for the abuse limits, under `app.rate-limit`.
 *
 * Externalised because the right numbers depend on the deployment: how many users sit behind one
 * NAT, what an SMS costs, how aggressive the traffic is. The defaults are deliberately generous
 * enough for a person fumbling a password and far too tight for automated abuse.
 */
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
class RateLimitProperties {

    /**
     * Turn the limits off entirely. Intended for test runs, where the whole suite shares one client
     * address and would otherwise throttle itself. Leave on in every real deployment.
     */
    var enabled: Boolean = true

    /**
     * How many reverse proxies of your own stand between the internet and this server.
     *
     * Set this to the real number, or the limits below can be walked straight past. It is what tells
     * [ClientIpResolver] how far in from the right of `X-Forwarded-For` the genuine client address
     * sits — everything to the left of that came from the caller and means nothing.
     *
     * 0 (the default) means the server is reached directly and no forwarded header is believed at
     * all. One proxy — nginx, Cloudflare, a cloud load balancer — is 1. Getting this *too high* is
     * the safe direction to be wrong in: the resolver falls back to the connection's own peer
     * address. Getting it too low attributes traffic to your proxy, and a single global bucket will
     * lock everybody out at once, so the mistake shows itself immediately.
     *
     * Setting this above 0 also requires `server.forward-headers-strategy: framework`, since that is
     * what makes the rest of the application see the client's scheme and host.
     */
    var trustedProxyCount: Int = 0

    /** Password and OTP sign-in attempts, per client address. */
    var login: Rule = Rule(limit = 10, window = Duration.ofMinutes(5))

    /** Requests to send a code. Each one spends money or mail reputation. */
    var otpSend: Rule = Rule(limit = 5, window = Duration.ofMinutes(15))

    /** Attempts to redeem a code, which is what makes guessing one impractical. */
    var otpVerify: Rule = Rule(limit = 10, window = Duration.ofMinutes(15))

    /** Password-reset mail requests, per client address. */
    var passwordResetRequest: Rule = Rule(limit = 5, window = Duration.ofHours(1))

    /** Submissions of a reset form. */
    var passwordResetSubmit: Rule = Rule(limit = 10, window = Duration.ofHours(1))

    /**
     * Codes sent to one address or phone number, whoever asks. The per-address limits above don't
     * stop a distributed sender from flooding a single victim.
     */
    var otpPerTarget: Rule = Rule(limit = 5, window = Duration.ofHours(1))

    class Rule(
        var limit: Int = 10,
        var window: Duration = Duration.ofMinutes(5)
    )
}
