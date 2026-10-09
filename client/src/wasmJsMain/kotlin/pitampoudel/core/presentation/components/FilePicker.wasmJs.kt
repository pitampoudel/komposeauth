package pitampoudel.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import pitampoudel.core.domain.KmpFile
import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.files.File
import org.w3c.files.FileReader

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
            val picked = arrayOfNulls<KmpFile>(files.size)
            var remaining = files.size
            files.forEachIndexed { index, file ->
                read(file) { kmpFile ->
                    picked[index] = kmpFile
                    if (--remaining == 0) onPicked(picked.filterNotNull())
                }
            }
        }
        inputElement.click()
    }

    private fun read(file: File, onRead: (KmpFile?) -> Unit) {
        val reader = FileReader()
        reader.onload = {
            val bytes = Int8Array(reader.result!!.unsafeCast<ArrayBuffer>())
            onRead(
                KmpFile(
                    byteArray = ByteArray(bytes.length) { bytes[it] },
                    mimeType = file.type,
                    name = file.name,
                )
            )
        }
        reader.onerror = { onRead(null) }
        reader.readAsArrayBuffer(file)
    }
}