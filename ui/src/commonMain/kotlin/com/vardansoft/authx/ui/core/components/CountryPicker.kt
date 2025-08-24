package com.vardansoft.authx.ui.core.components

import androidx.compose.runtime.Composable


@Composable
expect fun CountryPicker(
    countryNameCode: String,
    onCountryNameCodeChanged: (String) -> Unit
)