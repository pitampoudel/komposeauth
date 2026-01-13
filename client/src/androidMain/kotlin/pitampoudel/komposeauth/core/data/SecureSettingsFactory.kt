package pitampoudel.komposeauth.core.data

import android.content.Context
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable

internal class SecureSettingsFactory() {
    fun create(context: Context): ObservableSettings {
        return Settings().makeObservable()
    }
}
