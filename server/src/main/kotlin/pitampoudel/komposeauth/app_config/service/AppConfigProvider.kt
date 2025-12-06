package pitampoudel.komposeauth.app_config.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.domain.Platform
import pitampoudel.komposeauth.webauthn.utils.WebAuthnUtils.androidOrigin
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.Collections

@Service
class AppConfigProvider(val appConfigService: AppConfigService) {

    val selfBaseUrl: String
        get() {
            val cfg = appConfigService.getEnv().selfBaseUrl?.trim()
            return cfg ?: "http://${getLocalIpAddress()}:8080"
        }

    fun getLocalIpAddress(): String? {
        val excludePrefixes = listOf(
            "veth", "docker", "virbr", "br-", "tun", "tap", "ppp", "vpn", "vmnet", "vboxnet"
        )

        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .sortedBy { it.index } // lower index usually means physical NIC

        for (ni in interfaces) {
            val name = ni.name.lowercase()
            if (excludePrefixes.any { name.startsWith(it) }) continue

            for (addr in Collections.list(ni.inetAddresses)) {
                if (addr is Inet4Address && !addr.isLoopbackAddress) {
                    return addr.hostAddress
                }
            }
        }
        return null
    }

    fun rpId(): String {
        return appConfigService.getEnv().rpId ?: URL(selfBaseUrl).host
    }

    val name: String get() = appConfigService.getEnv().name ?: "komposeauth"
    val logoUrl: String? get() = appConfigService.getEnv().logoUrl
    val facebookLink : String? get() = appConfigService.getEnv().facebookLink
    val instagramLink : String? get() = appConfigService.getEnv().instagramLink
    val tiktokLink : String? get() = appConfigService.getEnv().tiktokLink
    val linkedinLink : String? get() = appConfigService.getEnv().linkedinLink
    val youtubeLink : String? get() = appConfigService.getEnv().youtubeLink
    val privacyLink : String? get() = appConfigService.getEnv().privacyLink

    val gcpProjectId: String? get() = appConfigService.getEnv().gcpProjectId
    val gcpBucketName: String? get() = appConfigService.getEnv().gcpBucketName
    val googleAuthClientId: String? get() = appConfigService.getEnv().googleAuthClientId
    val googleAuthClientSecret: String? get() = appConfigService.getEnv().googleAuthClientSecret
    val googleAuthDesktopClientId: String? get() = appConfigService.getEnv().googleAuthDesktopClientId
    val googleAuthDesktopClientSecret: String? get() = appConfigService.getEnv().googleAuthDesktopClientSecret
    val allowedAndroidSha256List: String? get() = appConfigService.getEnv().allowedAndroidSha256List
    val corsAllowedOriginList: String? get() = appConfigService.getEnv().corsAllowedOriginList
    val twilioAccountSid: String? get() = appConfigService.getEnv().twilioAccountSid
    val twilioAuthToken: String? get() = appConfigService.getEnv().twilioAuthToken
    val twilioFromNumber: String? get() = appConfigService.getEnv().twilioFromNumber
    val twilioVerifyServiceSid: String? get() = appConfigService.getEnv().twilioVerifyServiceSid
    val smtpHost: String? get() = appConfigService.getEnv().smtpHost
    val smtpPort: Int? get() = appConfigService.getEnv().smtpPort
    val smtpUsername: String? get() = appConfigService.getEnv().smtpUsername
    val smtpPassword: String? get() = appConfigService.getEnv().smtpPassword
    val smtpFromEmail: String? get() = appConfigService.getEnv().smtpFromEmail
    val smtpFromName: String get() = appConfigService.getEnv().smtpFromName ?: name
    val brandColor: String get() = appConfigService.getEnv().brandColor ?: "#4F46E5"
    val supportEmail: String
        get() = appConfigService.getEnv().supportEmail ?: (smtpFromEmail ?: "support@${rpId()}")
    val emailFooterText: String
        get() = appConfigService.getEnv().emailFooterText ?: "© ${name}. All rights reserved."
    val sentryDsn: String? get() = appConfigService.getEnv().sentryDsn
    val samayeApiKey: String? = appConfigService.getEnv().samayeApiKey
    fun googleClientId(platform: Platform): String? {
        val value = when (platform) {
            Platform.DESKTOP -> googleAuthDesktopClientId
            Platform.WEB -> googleAuthClientId
            Platform.ANDROID -> googleAuthClientId
            Platform.IOS -> googleAuthClientId
        }
        return value?.takeIf { it.isNotBlank() }
    }

    fun googleClientSecret(platform: Platform): String? {
        val value = when (platform) {
            Platform.DESKTOP -> googleAuthDesktopClientSecret
            Platform.WEB -> googleAuthClientSecret
            Platform.ANDROID -> googleAuthClientSecret
            Platform.IOS -> googleAuthClientSecret
        }
        return value?.takeIf { it.isNotBlank() }
    }

    fun corsAllowedOrigins(): List<String> {
        return corsAllowedOriginList?.split(",").orEmpty()
    }

    fun webauthnAllowedOrigins(): Set<String> {
        return allowedAndroidSha256List?.split(",")?.map {
            androidOrigin(it)
        }.orEmpty().toSet() + corsAllowedOrigins() + selfBaseUrl
    }


}