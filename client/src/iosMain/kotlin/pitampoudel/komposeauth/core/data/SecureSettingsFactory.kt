package pitampoudel.komposeauth.core.data

import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.observable.makeObservable

internal class SecureSettingsFactory() {
    fun create(): ObservableSettings {
        return KeychainSettings(service = "secure_settings").makeObservable()
    }
}