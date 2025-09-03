package com.vardansoft.authx.ui.core.components

import androidx.compose.runtime.Composable
import com.vardansoft.core.domain.KmpFile

interface FilePicker {
    fun launch()
}

@Composable
expect fun rememberFilePicker(
    input: String,
    onPicked: (KmpFile) -> Unit
): FilePicker
