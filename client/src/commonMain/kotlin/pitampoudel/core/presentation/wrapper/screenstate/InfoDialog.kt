package pitampoudel.core.presentation.wrapper.screenstate

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.pitampoudel.client.generated.resources.Res
import io.github.pitampoudel.client.generated.resources.checked
import io.github.pitampoudel.client.generated.resources.ok
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pitampoudel.core.presentation.GREEN
import pitampoudel.core.presentation.InfoMessage

@OptIn(ExperimentalResourceApi::class)
@Composable
fun InfoDialog(
    message: InfoMessage,
    onDismiss: () -> Unit
) {
    when (message) {
        is InfoMessage.Error -> AlertDialog(
            onDismissRequest = onDismiss,
            text = {
                Text(text = message.text)
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                    content = {
                        Text(
                            text = stringResource(Res.string.ok),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        )

        is InfoMessage.Success -> Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(10)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Image(
                        painterResource(Res.drawable.checked),
                        modifier = Modifier.size(40.dp),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GREEN)
                    )
                    Text(
                        message.text,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onDismiss) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            }
        }

        is InfoMessage.General -> AlertDialog(
            onDismissRequest = onDismiss,
            text = {
                Text(text = message.text)
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                    content = {
                        Text(
                            text = stringResource(Res.string.ok),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        )
    }

}
