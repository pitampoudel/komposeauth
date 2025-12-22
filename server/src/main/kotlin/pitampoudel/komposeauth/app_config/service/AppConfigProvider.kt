package pitampoudel.komposeauth.app_config.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.webauthn.utils.WebAuthnUtils.androidOrigin

@Service
class AppConfigProvider(val appConfigService: AppConfigService) {

    fun getConfig() = appConfigService.get()
    fun rpId(): String? {
        return appConfigService.get().rpId
    }

    fun googleClientId(platform: Platform): String? {
        val value = when (platform) {
            Platform.DESKTOP -> appConfigService.get().googleAuthDesktopClientId
            Platform.WEB -> appConfigService.get().googleAuthClientId
            Platform.ANDROID -> appConfigService.get().googleAuthClientId
            Platform.IOS -> appConfigService.get().googleAuthClientId
        }
        return value?.takeIf { it.isNotBlank() }
    }

    fun googleClientSecret(platform: Platform): String? {
        val value = when (platform) {
            Platform.DESKTOP -> appConfigService.get().googleAuthDesktopClientSecret
            Platform.WEB -> appConfigService.get().googleAuthClientSecret
            Platform.ANDROID -> appConfigService.get().googleAuthClientSecret
            Platform.IOS -> appConfigService.get().googleAuthClientSecret
        }
        return value?.takeIf { it.isNotBlank() }
    }

    fun corsAllowedOrigins(): List<String> {
        return appConfigService.get().corsAllowedOriginList?.split(",").orEmpty()
    }

    fun webauthnAllowedOrigins(): Set<String> {
        return appConfigService.get().allowedAndroidSha256List?.split(",")?.map {
            androidOrigin(it)
        }.orEmpty().toSet() + corsAllowedOrigins()
    }


}