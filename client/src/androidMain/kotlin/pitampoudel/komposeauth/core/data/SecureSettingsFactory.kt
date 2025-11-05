package pitampoudel.komposeauth.core.data

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable


internal actual class SecureSettingsFactory actual constructor() {
    actual fun create(): ObservableSettings {
        return Settings().makeObservable()
    }
}
