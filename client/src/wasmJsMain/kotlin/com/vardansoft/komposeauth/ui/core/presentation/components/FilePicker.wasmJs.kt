package com.vardansoft.komposeauth.ui.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vardansoft.core.domain.KmpFile
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList

@Composable
actual fun rememberFilePicker(
    input: List<String>,
    selectionMode: SelectionMode,
    onPicked: (List<KmpFile>) -> Unit
): FilePicker {
    return remember(input, selectionMode, onPicked) {
        FilePickerImpl(
            input = input,
            selectionMode = selectionMode,
            onPicked = onPicked,
        )
    }
}

private class FilePickerImpl(
    private val input: List<String>,
    private val selectionMode: SelectionMode,
    private val onPicked: (List<KmpFile>) -> Unit,
) : FilePicker {
    override fun launch() {
        val inputElement = document.createElement("input") as HTMLInputElement
        inputElement.type = "file"
        inputElement.multiple = selectionMode == SelectionMode.MULTIPLE
        inputElement.accept = input.joinToString(",")
        inputElement.onchange = { event ->
            val files = (event.target as HTMLInputElement).files?.asList() ?: emptyList()
            val kmpFiles = files.map { file ->
                KmpFile(
                    byteArray = byteArrayOf(),
                    mimeType = file.type,
                    name = file.name,
                )
            }
            onPicked(kmpFiles)
        }
        inputElement.click()
    }
}