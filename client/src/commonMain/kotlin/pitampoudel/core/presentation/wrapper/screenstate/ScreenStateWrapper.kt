package pitampoudel.core.presentation.wrapper.screenstate

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import pitampoudel.core.presentation.InfoMessage


@Composable
fun ScreenStateWrapper(
    loadingLogo: Painter? = null,
    progress: Float?,
    infoMessage: InfoMessage?,
    onDismissInfoMsg: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box {
            content()
            if (progress != null) {
                ProgressDialog(0.0F, loadingLogo)
            }
            if (infoMessage != null) {
                InfoDialog(infoMessage, onDismiss = onDismissInfoMsg)
            }
        }
    }
}
