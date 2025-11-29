package pitampoudel.komposeauth.app_config.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

@Document("config")
@TypeAlias("config")
data class AppConfig(
    @Id
    val id: String = SINGLETON_ID,

    var name: String? = null,
    var logoUrl: String? = null,
    var brandColor: String? = null,
    var supportEmail: String? = null,
    var selfBaseUrl: String? = null,
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
    var smtpFromEmail: String? = null,
    var smtpFromName: String? = null,
    var emailFooterText: String? = null,

    var sentryDsn: String? = null,
    // Third-party SMS provider (Samaye) API key
    var samayeApiKey: String? = null,
) {
    fun clean(): AppConfig {
        if (name.isNullOrBlank()) name = null
        if (logoUrl.isNullOrBlank()) logoUrl = null
        if (selfBaseUrl.isNullOrBlank()) selfBaseUrl = null
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
        if (samayeApiKey.isNullOrBlank()) samayeApiKey = null
        return this
    }

    companion object {
        const val SINGLETON_ID: String = "singleton"
    }
}