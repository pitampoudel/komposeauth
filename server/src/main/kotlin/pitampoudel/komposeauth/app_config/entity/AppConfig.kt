package pitampoudel.komposeauth.app_config.entity

import jakarta.validation.constraints.Email
import org.hibernate.validator.constraints.URL
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("config")
@TypeAlias("config")
data class AppConfig(
    @Id
    val id: String = SINGLETON_ID,

    var name: String? = null,
    @field:URL(message = "Logo URL must be a valid URL")
    var logoUrl: String? = null,
    var brandColor: String? = null,
    @field:URL(message = "Website URL must be a valid URL")
    var websiteUrl:String? = null,
    @field:URL(message = "Facebook link must be a valid URL")
    var facebookLink:String? = null,
    @field:URL(message = "Instagram link must be a valid URL")
    var instagramLink:String? = null,
    @field:URL(message = "TikTok link must be a valid URL")
    var tiktokLink:String? = null,
    @field:URL(message = "LinkedIn link must be a valid URL")
    var linkedinLink:String? = null,
    @field:URL(message = "YouTube link must be a valid URL")
    var youtubeLink:String? = null,
    @field:URL(message = "Privacy link must be a valid URL")
    var privacyLink:String? = null,

    @field:Email(message = "Invalid support email address")
    var supportEmail: String? = null,
    var rpId: String? = null,
    var gcpProjectId: String? = null,
    var gcpBucketName: String? = null,

    var googleAuthClientId: String? = null,
    var googleAuthClientSecret: String? = null,
    var googleAuthDesktopClientId: String? = null,
    var googleAuthDesktopClientSecret: String? = null,
    var appleAuthClientId: String? = null,

    var allowedAndroidSha256List: String? = null,
    var corsAllowedOriginList: String? = null,

    /**
     * How many reverse proxies of your own stand between the internet and this server.
     *
     * Set this to the real number, or the abuse limits below can be walked straight past. It is
     * what tells [pitampoudel.komposeauth.core.security.ratelimit.ClientIpResolver] how far in
     * from the right of `X-Forwarded-For` the genuine client address sits — everything to the
     * left of that came from the caller and means nothing.
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
    var trustedProxyCount: Int? = null,

    /**
     * Name of a header the platform guarantees, holding the client address on its own.
     *
     * Some edges do not merely append to `X-Forwarded-For`, they *replace* what the caller sent and
     * publish the address they observed under a header of their own — `X-Envoy-External-Address` on
     * Railway, `Fly-Client-IP` on Fly, `CF-Connecting-IP` behind Cloudflare. Where that is offered it
     * is the better signal: one value, written by the edge, with no positions to count and nothing
     * of the caller's left in it. [trustedProxyCount] is then unnecessary and is not consulted.
     *
     * Only set this for a header your own edge writes. Naming one the platform does not overwrite
     * makes the limits worthless, because then the caller is simply telling you who to count.
     */
    var clientIpHeader: String? = null,

    /**
     * Comma- or newline-separated role names that may be granted to users, on top of the
     * built-in [pitampoudel.komposeauth.core.domain.Roles.BUILT_IN] roles.
     */
    var rolesCatalog: String? = null,

    // SMS Provider Configuration
    var smsProvider: String? = null, // "twilio", "samaye", "sparrow", "whatsapp", or null for none

    var twilioAccountSid: String? = null,
    var twilioAuthToken: String? = null,
    var twilioFromNumber: String? = null,
    var twilioVerifyServiceSid: String? = null,

    // SMTP
    var smtpHost: String? = null,
    var smtpPort: Int? = null,
    var smtpUsername: String? = null,
    var smtpPassword: String? = null,
    @field:Email(message = "Invalid SMTP from email address")
    var smtpFromEmail: String? = null,
    var smtpFromName: String? = null,
    var emailFooterText: String? = null,

    var sentryDsn: String? = null,
    var slackBotToken: String? = null,
    var slackChannelId: String? = null,
    // Third-party SMS provider (Samaye) API key
    var samayeApiKey: String? = null,

    // Third-party SMS provider (Sparrow) API key
    var sparrowApiToken: String? = null,
    var sparrowFromNumber: String? = null,

    // WhatsApp Business Cloud API
    var whatsappAccessToken: String? = null,
    var whatsappPhoneNumberId: String? = null,

    // Third-factor KYC
    @field:URL(message = "Third-factor URL must be a valid URL")
    var thirdFactorUrl: String? = null,
    var thirdFactorSecretKey: String? = null,
    var thirdFactorToken: String? = null,

    @CreatedDate
    val createdAt: Instant = Instant.now(),
    @LastModifiedDate
    val updatedAt: Instant = Instant.now()
) {
    fun clean(): AppConfig {
        if (name.isNullOrBlank()) name = null
        if (websiteUrl.isNullOrBlank()) websiteUrl = null
        if (facebookLink.isNullOrBlank()) facebookLink = null
        if (instagramLink.isNullOrBlank()) instagramLink = null
        if (tiktokLink.isNullOrBlank()) tiktokLink = null
        if (linkedinLink.isNullOrBlank()) linkedinLink = null
        if (youtubeLink.isNullOrBlank()) youtubeLink = null
        if (privacyLink.isNullOrBlank()) privacyLink = null

        if (logoUrl.isNullOrBlank()) logoUrl = null
        if (gcpProjectId.isNullOrBlank()) gcpProjectId = null
        if (gcpBucketName.isNullOrBlank()) gcpBucketName = null
        if (googleAuthClientId.isNullOrBlank()) googleAuthClientId = null
        if (googleAuthClientSecret.isNullOrBlank()) googleAuthClientSecret = null
        if (googleAuthDesktopClientId.isNullOrBlank()) googleAuthDesktopClientId = null
        if (googleAuthDesktopClientSecret.isNullOrBlank()) googleAuthDesktopClientSecret = null
        if (appleAuthClientId.isNullOrBlank()) appleAuthClientId = null
        if (allowedAndroidSha256List.isNullOrBlank()) allowedAndroidSha256List = null
        if (corsAllowedOriginList.isNullOrBlank()) corsAllowedOriginList = null
        if (clientIpHeader.isNullOrBlank()) clientIpHeader = null
        if ((trustedProxyCount ?: 0) <= 0) trustedProxyCount = null
        if (rolesCatalog.isNullOrBlank()) rolesCatalog = null
        if (smsProvider.isNullOrBlank()) smsProvider = null
        if (twilioAccountSid.isNullOrBlank()) twilioAccountSid = null
        if (twilioAuthToken.isNullOrBlank()) twilioAuthToken = null
        if (twilioFromNumber.isNullOrBlank()) twilioFromNumber = null
        if (twilioVerifyServiceSid.isNullOrBlank()) twilioVerifyServiceSid = null
        if (smtpHost.isNullOrBlank()) smtpHost = null
        if (smtpPort == null) smtpPort = null
        if (smtpUsername.isNullOrBlank()) smtpUsername = null
        if (smtpPassword.isNullOrBlank()) smtpPassword = null
        if (smtpFromEmail.isNullOrBlank()) smtpFromEmail = null
        if (smtpFromName.isNullOrBlank()) smtpFromName = null
        if (brandColor.isNullOrBlank()) brandColor = null
        if (supportEmail.isNullOrBlank()) supportEmail = null
        if (emailFooterText.isNullOrBlank()) emailFooterText = null
        if (sentryDsn.isNullOrBlank()) sentryDsn = null
        if (slackBotToken.isNullOrBlank()) slackBotToken = null
        if (slackChannelId.isNullOrBlank()) slackChannelId = null
        if (samayeApiKey.isNullOrBlank()) samayeApiKey = null
        if (sparrowApiToken.isNullOrBlank()) sparrowApiToken = null
        if (sparrowFromNumber.isNullOrBlank()) sparrowFromNumber = null
        if (whatsappAccessToken.isNullOrBlank()) whatsappAccessToken = null
        if (whatsappPhoneNumberId.isNullOrBlank()) whatsappPhoneNumberId = null
        if (thirdFactorUrl.isNullOrBlank()) thirdFactorUrl = null
        if (thirdFactorSecretKey.isNullOrBlank()) thirdFactorSecretKey = null
        if (thirdFactorToken.isNullOrBlank()) thirdFactorToken = null
        return this
    }

    companion object {
        const val SINGLETON_ID: String = "singleton"
    }
}
