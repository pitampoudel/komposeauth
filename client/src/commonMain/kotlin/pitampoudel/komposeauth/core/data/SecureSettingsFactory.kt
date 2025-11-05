package pitampoudel.komposeauth.core.data

import com.russhwolf.settings.ObservableSettings

/**
 * Provides an [com.russhwolf.settings.ObservableSettings] instance backed by the most secure, OS-backed storage
 * available on the current platform (e.g., Android EncryptedSharedPreferences, iOS Keychain).
 *
 * Platforms without a secure option will fall back to the default [com.russhwolf.settings.Settings] backend.
 */
internal expect class SecureSettingsFactory {
    /**
     * Returns an [com.russhwolf.settings.ObservableSettings] instance. Implementation should use a secure
     * storage mechanism where available.
     */
    fun create(): ObservableSettings
}