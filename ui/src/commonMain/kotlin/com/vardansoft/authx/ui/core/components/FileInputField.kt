package com.vardansoft.authx.ui.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vardansoft.core.domain.KmpFile
import com.vardansoft.ui.generated.resources.Res
import com.vardansoft.ui.generated.resources.change_file
import com.vardansoft.ui.generated.resources.choose_file
import com.vardansoft.ui.generated.resources.file_not_selected
import com.vardansoft.ui.generated.resources.file_selected
import org.jetbrains.compose.resources.stringResource


@Composable
fun FileInputField(
    title: String,
    hint: String,
    mimeType: String,
    file: KmpFile?,
    enabled: Boolean,
    onSelected: (KmpFile?) -> Unit
) {
    val launcher = rememberFilePicker(mimeType) {
        onSelected(it)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val hasFile = file != null
        if (hasFile) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Preview if image
                val isImage = file.mimeType?.startsWith("image/") == true
                if (isImage) {
                    AsyncImage(
                        model = file.byteArray,
                        modifier = Modifier.size(96.dp),
                        contentDescription = null
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sizeKb = file.byteArray.size / 1024
                    val info = buildString {
                        append(stringResource(Res.string.file_selected))
                        file.mimeType?.let { append(" ($it)") }
                        append(" - ")
                        append(sizeKb)
                        append(" KB")
                    }
                    Text(
                        info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { onSelected(null) }, enabled = enabled) {
                        Icon(Icons.Filled.Close, contentDescription = "Reset file")
                    }
                }
            }
        } else {
            Text(
                text = stringResource(Res.string.file_not_selected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = { launcher.launch() }, enabled = enabled) {
            Text(if (hasFile) stringResource(Res.string.change_file) else stringResource(Res.string.choose_file))
        }
    }
}