package com.vardansoft.authx.ui.core.components

import androidx.compose.runtime.Composable
import com.vardansoft.core.domain.KmpFile

@Composable
actual fun rememberFilePicker(
    input: List<String>,
    selectionMode: SelectionMode,
    onPicked: (List<KmpFile>) -> Unit
): FilePicker {
    TODO("Not yet implemented")
}