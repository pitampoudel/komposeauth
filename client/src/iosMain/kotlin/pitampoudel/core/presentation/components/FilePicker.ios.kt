package pitampoudel.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import pitampoudel.core.domain.KmpFile
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import platform.darwin.NSObject

@Composable
actual fun rememberFilePicker(
    input: List<String>,
    selectionMode: SelectionMode,
    onPicked: (List<KmpFile>) -> Unit
): FilePicker {
    val delegate = remember { FilePickerDelegate(onPicked) }

    return remember {
        IosFilePicker(
            input = input,
            selectionMode = selectionMode,
            delegate = delegate
        )
    }
}

private class IosFilePicker(
    private val input: List<String>,
    private val selectionMode: SelectionMode,
    private val delegate: FilePickerDelegate
) : FilePicker {

    override fun launch() {
        val types = input.ifEmpty { listOf(UTTypeData.identifier) }
        val picker = UIDocumentPickerViewController(
            documentTypes = types,
            inMode = UIDocumentPickerModeImport
        ).apply {
            allowsMultipleSelection = (selectionMode == SelectionMode.MULTIPLE)
            this.delegate = delegate
        }

        val controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        controller?.presentViewController(picker, animated = true, completion = null)
    }
}

private class FilePickerDelegate(
    private val onPicked: (List<KmpFile>) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        val files = didPickDocumentsAtURLs.mapNotNull { url ->
            (url as? NSURL)?.let { toKmpFile(it) }
        }
        onPicked(files)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        // User cancelled; no-op
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun toKmpFile(url: NSURL): KmpFile {
        val fileName = url.lastPathComponent ?: "unknown"
        val data = NSData.create(contentsOfURL = url)
        val byteArray = data?.let { nsData ->
            ByteArray(nsData.length.toInt()).apply {
                usePinned {
                    platform.posix.memcpy(it.addressOf(0), nsData.bytes, nsData.length)
                }
            }
        } ?: ByteArray(0)

        val mimeType = detectMimeType(url)

        return KmpFile(
            name = fileName,
            byteArray = byteArray,
            mimeType = mimeType
        )
    }

    private fun detectMimeType(url: NSURL): String {
        // Try UTType first (iOS 14+)
        val type = url.pathExtension?.let { ext ->
            UTTypeCreatePreferredIdentifierForTag(
                kUTTagClassFilenameExtension,
                ext as NSString,
                null
            )?.takeIf { it != null }?.let { uti ->
                UTTypeCopyPreferredTagWithClass(uti.takeRetainedValue(), kUTTagClassMIMEType)
                    ?.takeRetainedValue()?.toString()
            }
        }

        if (type != null) return type

        // Simple fallback
        val ext = url.pathExtension?.lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            else -> "application/octet-stream"
        }
    }
}
