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
    onPicked: (KmpFile) -> Unit
): FilePicker {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mime = context.contentResolver.getType(uri)
            if (bytes != null) {
                onPicked(KmpFile(bytes, mime))
            }
        }
    }
    return remember {
        object : FilePicker {
            override fun launch() {
                launcher.launch(input)
            }
        }
    }
}

