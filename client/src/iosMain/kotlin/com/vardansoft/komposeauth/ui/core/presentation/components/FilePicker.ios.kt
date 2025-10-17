package com.vardansoft.komposeauth.ui.core.presentation.components

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