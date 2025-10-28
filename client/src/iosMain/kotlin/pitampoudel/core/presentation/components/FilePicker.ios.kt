package pitampoudel.core.presentation.components

import androidx.compose.runtime.Composable
import pitampoudel.core.domain.KmpFile

@Composable
actual fun rememberFilePicker(
    input: List<String>,
    selectionMode: SelectionMode,
    onPicked: (List<KmpFile>) -> Unit
): FilePicker {
    TODO("Not yet implemented")
}