package pitampoudel.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import pitampoudel.core.domain.KmpFile
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files

@Composable
actual fun rememberFilePicker(
    input: List<String>,
    selectionMode: SelectionMode,
    onPicked: (List<KmpFile>) -> Unit
): FilePicker {
    return remember {
        object : FilePicker {
            override fun launch() {
                val dialog = FileDialog(null as Frame?, "Select File")
                dialog.isVisible = true
                val dir = dialog.directory
                val file = dialog.file
                if (dir != null && file != null) {
                    val f = File(dir, file)
                    val bytes = Files.readAllBytes(f.toPath())
                    val mime = Files.probeContentType(f.toPath())
                    onPicked(listOf(KmpFile(byteArray = bytes, mimeType = mime, name = f.name)))
                }
            }
        }
    }
}