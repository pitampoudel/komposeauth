package com.vardansoft.authx.ui.core.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.hbb20.CountryCodePicker

@Composable
actual fun CountryPicker(
    countryNameCode: String,
    onCountryNameCodeChanged: (String) -> Unit
) {
    val color = LocalContentColor.current
    AndroidView(factory = {
        val countryCodePicker = CountryCodePicker(it)
        countryCodePicker.contentColor = color.toArgb()
        countryCodePicker.setAutoDetectedCountry(true)
        countryCodePicker.setCountryForNameCode(countryNameCode)
        onCountryNameCodeChanged(countryCodePicker.selectedCountryNameCode)
        countryCodePicker.setOnCountryChangeListener {
            onCountryNameCodeChanged(countryCodePicker.selectedCountryNameCode)
        }
        countryCodePicker
    }) {
        it.setCountryForNameCode(countryNameCode)
    }
}