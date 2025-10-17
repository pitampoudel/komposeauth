package com.vardansoft.core.presentation.components

import androidx.compose.runtime.Composable


@Composable
expect fun CountryPicker(
    countryNameCode: String,
    onCountryNameCodeChanged: (String) -> Unit
)