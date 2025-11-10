package pitampoudel.core.presentation.wrapper.screenstate

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import pitampoudel.core.presentation.InfoMessage


interface ScreenStateConfig {
    val loadingLogo: Painter?

    @Composable
    fun ProgressDialog(progress: Float, onDismissProgress: (() -> Unit)?) {
        ProgressDialog(
            progress = progress,
            logo = loadingLogo,
            onDismissProgress = onDismissProgress
        )
    }

    @Composable
    fun InfoDialog(
        infoMessage: InfoMessage,
        onDismiss: () -> Unit,
    ) {
        InfoDialog(infoMessage, onDismiss = onDismiss)
    }
}

@Composable
fun ScreenStateWrapper(
    config: ScreenStateConfig,
    progress: Float? = null,
    onDismissProgress: (() -> Unit)? = null,
    infoMessage: InfoMessage?,
    onDismissInfoMsg: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box {
            content()
            if (progress != null) {
                config.ProgressDialog(progress = progress, onDismissProgress = onDismissProgress)
            }
            if (infoMessage != null) {
                config.InfoDialog(infoMessage = infoMessage, onDismiss = onDismissInfoMsg)
            }
        }
    }
}
