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

    var allowedAndroidSha256List: String? = null,
    var corsAllowedOriginList: String? = null,

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
    @field:URL(message = "Slack webhook must be a valid URL")
    var slackWebhookUrl: String? = null,
    // Third-party SMS provider (Samaye) API key
    var samayeApiKey: String? = null,

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
        if (allowedAndroidSha256List.isNullOrBlank()) allowedAndroidSha256List = null
        if (corsAllowedOriginList.isNullOrBlank()) corsAllowedOriginList = null
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
        if (slackWebhookUrl.isNullOrBlank()) slackWebhookUrl = null
        if (samayeApiKey.isNullOrBlank()) samayeApiKey = null
        return this
    }

    companion object {
        const val SINGLETON_ID: String = "singleton"
    }
}