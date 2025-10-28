package pitampoudel.core.presentation.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlinx.browser.window

@Composable
actual fun CountryPicker(
    countryNameCode: String,
    onCountryNameCodeChanged: (String) -> Unit
) {
    TextButton(onClick = {
        val input = window.prompt("Enter ISO country code (e.g., US, IN, NP)", countryNameCode)
        val normalized = input?.trim()?.uppercase()
        if (!normalized.isNullOrEmpty() && normalized.length in 2..3 && normalized.all { it.isLetter() }) {
            onCountryNameCodeChanged(normalized)
        }
    }) {
        Text(text = countryNameCode)
    }
}
