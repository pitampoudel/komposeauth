package pitampoudel.core.presentation.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.currentLocale
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

@Composable
actual fun CountryPicker(
    countryNameCode: String,
    onCountryNameCodeChanged: (String) -> Unit
) {
    TextButton(onClick = {
        val dismiss: () -> Unit = {
            topViewController()?.dismissViewControllerAnimated(true, null)
        }
        val onCountrySelected: (Country) -> Unit = { country ->
            onCountryNameCodeChanged(country.code)
            dismiss()
        }
        val countryListScreen = ComposeUIViewController {
            CountryListScreen(
                countries = getCountries(),
                onCountrySelected = onCountrySelected,
                onDismiss = dismiss
            )
        }
        topViewController()?.presentViewController(countryListScreen, animated = true, completion = null)
    }) {
        Text(text = countryNameCode)
    }
}

private fun topViewController(): UIViewController? {
    val app = UIApplication.sharedApplication
    val window = app.keyWindow ?: (app.windows.first() as? UIWindow)
    var current = window?.rootViewController
    while (current?.presentedViewController != null) {
        current = current.presentedViewController
    }
    return current
}

private fun getCountries(): List<Country> {
    val currentLocale = NSLocale.currentLocale
    return (NSLocale.availableLocaleIdentifiers as List<String>).mapNotNull { identifier ->
        val locale = NSLocale(identifier)
        val countryCode = locale.objectForKey(NSLocaleCountryCode) as? String
        countryCode?.let {
            val countryName = currentLocale.displayNameForKey(NSLocaleCountryCode, it) ?: it
            Country(name = countryName, code = it)
        }
    }.distinctBy { it.code }.sortedBy { it.name }
}
