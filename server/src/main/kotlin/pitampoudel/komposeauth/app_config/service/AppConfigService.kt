package pitampoudel.komposeauth.app_config.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.webauthn.utils.WebAuthnUtils.androidOrigin

@Service
class AppConfigService(val appConfigProvider: AppConfigProvider) {

    fun getConfig() = appConfigProvider.get()
    fun rpId(): String? {
        return appConfigProvider.get().rpId
    }

    fun googleClientId(platform: Platform): String? {
        val value = when (platform) {
            Platform.DESKTOP -> appConfigProvider.get().googleAuthDesktopClientId
            Platform.WEB -> appConfigProvider.get().googleAuthClientId
            Platform.ANDROID -> appConfigProvider.get().googleAuthClientId
            Platform.IOS -> appConfigProvider.get().googleAuthClientId
        }
        return value?.takeIf { it.isNotBlank() }
    }

    fun googleClientSecret(platform: Platform): String? {
        val value = when (platform) {
            Platform.DESKTOP -> appConfigProvider.get().googleAuthDesktopClientSecret
            Platform.WEB -> appConfigProvider.get().googleAuthClientSecret
            Platform.ANDROID -> appConfigProvider.get().googleAuthClientSecret
            Platform.IOS -> appConfigProvider.get().googleAuthClientSecret
        }
        return value?.takeIf { it.isNotBlank() }
    }

    fun corsAllowedOrigins(): List<String> {
        return appConfigProvider.get().corsAllowedOriginList?.split(",").orEmpty()
    }

    fun webauthnAllowedOrigins(): Set<String> {
        return appConfigProvider.get().allowedAndroidSha256List?.split(",")?.map {
            androidOrigin(it)
        }.orEmpty().toSet() + corsAllowedOrigins()
    }


}