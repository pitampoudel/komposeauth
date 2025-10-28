package pitampoudel.core.presentation.wrapper.screenstate

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import pitampoudel.core.presentation.InfoMessage


@Composable
fun ScreenStateWrapper(
    progress: Float?,
    infoMessage: InfoMessage?,
    onDismissInfoMsg: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box {
            content()
            if (progress != null) {
                ProgressDialog(progress)
            }
            if (infoMessage != null) {
                InfoDialog(infoMessage, onDismiss = onDismissInfoMsg)
            }
        }
    }
}
