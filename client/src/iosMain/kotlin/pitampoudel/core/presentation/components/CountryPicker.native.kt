package pitampoudel.core.presentation.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import platform.Foundation.firstObject
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UITextField
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

@Composable
actual fun CountryPicker(
    countryNameCode: String,
    onCountryNameCodeChanged: (String) -> Unit
) {
    TextButton(onClick = {
        presentCountryCodePrompt(
            initial = countryNameCode,
            onSubmit = { code ->
                val normalized = code.trim().uppercase()
                if (normalized.isNotEmpty() && normalized.length in 2..3 && normalized.all { it.isLetter() }) {
                    onCountryNameCodeChanged(normalized)
                }
            }
        )
    }) {
        Text(text = countryNameCode)
    }
}

private fun presentCountryCodePrompt(initial: String, onSubmit: (String) -> Unit) {
    val alert = UIAlertController.alertControllerWithTitle(
        title = "Country Code",
        message = "Enter ISO country code (e.g., US, IN, NP)",
        preferredStyle = UIAlertControllerStyleAlert
    )
    alert.addTextFieldWithConfigurationHandler { tf: UITextField? ->
        tf?.text = initial
        tf?.placeholder = "US"
        // Autocapitalization type may vary across SDKs; omit to avoid type issues
    }

    alert.addAction(
        UIAlertAction.actionWithTitle(
            title = "Cancel",
            style = UIAlertActionStyleCancel,
            handler = null
        )
    )

    alert.addAction(
        UIAlertAction.actionWithTitle(
        title = "OK",
        style = UIAlertActionStyleDefault,
        handler = { _ ->
            val tf = alert.textFields?.first() as? UITextField
            val value = tf?.text ?: ""
            onSubmit(value)
        }
    ))

    val top = topViewController()
    top?.presentViewController(alert, animated = true, completion = null)
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