package com.vardansoft.authx.ui.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vardansoft.core.domain.KmpFile
import com.vardansoft.ui.generated.resources.Res
import com.vardansoft.ui.generated.resources.change_file
import com.vardansoft.ui.generated.resources.choose_file
import com.vardansoft.ui.generated.resources.file_not_selected
import com.vardansoft.ui.generated.resources.file_selected
import com.vardansoft.ui.generated.resources.file_selected_size
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
    val launcher = rememberFilePicker(mimeType, SelectionMode.SINGLE) { selectedFiles ->
        onSelected(selectedFiles.firstOrNull())
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (file != null) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = stringResource(Res.string.file_selected),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusText = if (file != null) {
                    val sizeKb = file.byteArray.size / 1024
                    stringResource(Res.string.file_selected_size, sizeKb)
                } else {
                    stringResource(Res.string.file_not_selected)
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (file != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = { launcher.launch() },
                    enabled = enabled
                ) {
                    Text(
                        if (file != null) stringResource(Res.string.change_file) else stringResource(
                            Res.string.choose_file
                        )
                    )
                }
            }
        }
    }
}