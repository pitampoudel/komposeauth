package com.vardansoft.komposeauth.ui.core.presentation.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.vardansoft.core.domain.KmpFile

@Composable
actual fun rememberFilePicker(
    input: List<String>,
    selectionMode: SelectionMode,
    onPicked: (List<KmpFile>) -> Unit
): FilePicker {
    val context = LocalContext.current
    val singleLauncher = rememberLauncherForActivityResult(
        contract = GetContentWithMimeTypes
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mime = context.contentResolver.getType(uri)
            if (bytes != null && mime != null) {
                onPicked(
                    listOf(
                        KmpFile(
                            byteArray = bytes,
                            mimeType = mime,
                            name = uri.getFileName(context)
                        )
                    )
                )
            }
        }
    }

    val multipleLauncher = rememberLauncherForActivityResult(
        contract = GetMultipleContentsWithMimeTypes
    ) { uris ->
        val files = uris.mapNotNull { uri ->
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val mime = context.contentResolver.getType(uri)
            if (bytes != null && mime != null) KmpFile(
                byteArray = bytes,
                mimeType = mime,
                name = uri.getFileName(context)
            ) else null
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

private fun Uri.getFileName(context: Context): String {
    return context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        it.getString(nameIndex)
    } ?: lastPathSegment ?: "unknown"
}

private object GetContentWithMimeTypes : ActivityResultContract<List<String>, Uri?>() {
    override fun createIntent(context: Context, input: List<String>): Intent {
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .apply {
                when {
                    input.isEmpty() -> type = "*/*"
                    input.size == 1 -> type = input.first()
                    else -> {
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, input.toTypedArray())
                    }
                }
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (intent == null || resultCode != Activity.RESULT_OK) return null
        return intent.data
    }
}

private object GetMultipleContentsWithMimeTypes :
    ActivityResultContract<List<String>, List<Uri>>() {
    override fun createIntent(context: Context, input: List<String>): Intent {
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            .apply {
                when {
                    input.isEmpty() -> type = "*/*"
                    input.size == 1 -> type = input.first()
                    else -> {
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, input.toTypedArray())
                    }
                }
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (intent == null || resultCode != Activity.RESULT_OK) return emptyList()
        return intent.clipData?.let { clipData ->
            (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
        } ?: intent.data?.let { listOf(it) } ?: emptyList()
    }
}
