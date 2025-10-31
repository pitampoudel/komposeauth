package pitampoudel.komposeauth.setup.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

@Document("app_config")
@TypeAlias("app_config")
data class AppConfig(
    @Id
    val id: String = SINGLETON_ID,

    var name: String? = null,
    var logoUrl: String? = null,
    var selfBaseUrl: String? = null,
    var expectedGcpProjectId: String? = null,
    var gcpBucketName: String? = null,

    var googleAuthClientId: String? = null,
    var googleAuthClientSecret: String? = null,
    var googleAuthDesktopClientId: String? = null,
    var googleAuthDesktopClientSecret: String? = null,

    var assetLinksJson: String? = null,

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

    var sentryDsn: String? = null,
) {
    companion object {
        const val SINGLETON_ID: String = "singleton"
    }
}