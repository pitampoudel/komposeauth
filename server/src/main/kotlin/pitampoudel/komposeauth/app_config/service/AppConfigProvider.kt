package pitampoudel.komposeauth.app_config.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.webauthn.utils.WebAuthnUtils.androidOrigin
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

@Service
class AppConfigProvider(val appConfigService: AppConfigService) {

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

    fun rpId(): String? {
        return appConfigService.get().rpId ?: getLocalIpAddress()
    }

    val name: String get() = appConfigService.get().name ?: "komposeauth"
    val logoUrl: String? get() = appConfigService.get().logoUrl
    val websiteUrl: String? get() = appConfigService.get().websiteUrl
    val facebookLink: String? get() = appConfigService.get().facebookLink
    val instagramLink: String? get() = appConfigService.get().instagramLink
    val tiktokLink: String? get() = appConfigService.get().tiktokLink
    val linkedinLink: String? get() = appConfigService.get().linkedinLink
    val youtubeLink: String? get() = appConfigService.get().youtubeLink
    val privacyLink: String? get() = appConfigService.get().privacyLink

    val gcpProjectId: String? get() = appConfigService.get().gcpProjectId
    val gcpBucketName: String? get() = appConfigService.get().gcpBucketName
    val googleAuthClientId: String? get() = appConfigService.get().googleAuthClientId
    val googleAuthClientSecret: String? get() = appConfigService.get().googleAuthClientSecret
    val googleAuthDesktopClientId: String? get() = appConfigService.get().googleAuthDesktopClientId
    val googleAuthDesktopClientSecret: String? get() = appConfigService.get().googleAuthDesktopClientSecret
    val allowedAndroidSha256List: String? get() = appConfigService.get().allowedAndroidSha256List
    val corsAllowedOriginList: String? get() = appConfigService.get().corsAllowedOriginList
    val twilioAccountSid: String? get() = appConfigService.get().twilioAccountSid
    val twilioAuthToken: String? get() = appConfigService.get().twilioAuthToken
    val twilioFromNumber: String? get() = appConfigService.get().twilioFromNumber
    val twilioVerifyServiceSid: String? get() = appConfigService.get().twilioVerifyServiceSid
    val smtpHost: String? get() = appConfigService.get().smtpHost
    val smtpPort: Int? get() = appConfigService.get().smtpPort
    val smtpUsername: String? get() = appConfigService.get().smtpUsername
    val smtpPassword: String? get() = appConfigService.get().smtpPassword
    val smtpFromEmail: String? get() = appConfigService.get().smtpFromEmail
    val smtpFromName: String get() = appConfigService.get().smtpFromName ?: name
    val brandColor: String get() = appConfigService.get().brandColor ?: "#4F46E5"
    val supportEmail: String
        get() = appConfigService.get().supportEmail ?: (smtpFromEmail ?: "support@${rpId()}")
    val emailFooterText: String
        get() = appConfigService.get().emailFooterText ?: "© ${name}. All rights reserved."
    val sentryDsn: String? get() = appConfigService.get().sentryDsn
    val samayeApiKey: String? = appConfigService.get().samayeApiKey
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
        }.orEmpty().toSet() + corsAllowedOrigins()
    }


}