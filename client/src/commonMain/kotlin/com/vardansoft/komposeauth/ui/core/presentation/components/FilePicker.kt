package com.vardansoft.komposeauth.ui.core.presentation.components

import androidx.compose.runtime.Composable
import com.vardansoft.core.domain.KmpFile

enum class SelectionMode {
    SINGLE,
    MULTIPLE
}

interface FilePicker {
    fun launch()
}

@Composable
expect fun rememberFilePicker(
    input: List<String> = emptyList(),
    selectionMode: SelectionMode = SelectionMode.SINGLE,
    onPicked: (List<KmpFile>) -> Unit
): FilePicker
