package com.vardansoft.authx.ui.core.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.vardansoft.core.domain.KmpFile

@Composable
actual fun rememberFilePicker(
    input: String,
    selectionMode: SelectionMode,
    onPicked: (List<KmpFile>) -> Unit
): FilePicker {
    val context = LocalContext.current
    val singleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mime = context.contentResolver.getType(uri)
            if (bytes != null && mime != null) {
                onPicked(listOf(KmpFile(bytes, mime)))
            }
        }
    }

    val multipleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val files = uris.mapNotNull { uri ->
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mime = context.contentResolver.getType(uri)
            if (bytes != null && mime != null) KmpFile(bytes, mime) else null
        }
        onPicked(files)
    }
    return remember {
        object : FilePicker {
            override fun launch() {
                when (selectionMode) {
                    SelectionMode.SINGLE -> singleLauncher.launch(input)
                    SelectionMode.MULTIPLE -> multipleLauncher.launch(input)
                }
            }
        }
    }
}

