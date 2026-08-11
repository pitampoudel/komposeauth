package pitampoudel.komposeauth.app_config.service

import org.springframework.stereotype.Service
import pitampoudel.komposeauth.core.domain.Platform
import pitampoudel.komposeauth.core.domain.Roles
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

    /** Role names configured for this app, normalized and stripped of anything malformed. */
    fun configuredRoles(): List<String> {
        return appConfigProvider.get().rolesCatalog
            ?.split(",", "\n")
            ?.map { Roles.normalize(it) }
            ?.filter { Roles.isValidName(it) }
            .orEmpty()
    }

    /** Every role that may be granted: the built-in ones plus whatever the app configured. */
    fun availableRoles(): List<String> {
        return (Roles.BUILT_IN + configuredRoles()).distinct()
    }

    /**
     * Configured browser origins, trimmed and de-duplicated.
     *
     * A bare `*` is dropped: these origins are used for credentialed CORS, where matching every
     * origin would let any site on the internet read authenticated responses. Narrower wildcard
     * patterns, such as one scoped to the subdomains of a single host, are kept.
     */
    fun corsAllowedOrigins(): List<String> {
        return appConfigProvider.get().corsAllowedOriginList
            ?.split(",", "\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && it != "*" }
            ?.distinct()
            .orEmpty()
    }

    fun webauthnAllowedOrigins(): Set<String> {
        return appConfigProvider.get().allowedAndroidSha256List?.split(",")?.map {
            androidOrigin(it)
        }.orEmpty().toSet() + corsAllowedOrigins()
    }


}