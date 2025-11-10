package pitampoudel.core.presentation.wrapper.screenstate

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import pitampoudel.core.presentation.InfoMessage


interface ScreenStateConfig {
    val logo: DrawableResource?

    @Composable
    fun ProgressDialog(progress: Float, onDismissProgress: (() -> Unit)?)

    @Composable
    fun InfoDialog(
        infoMessage: InfoMessage,
        onDismiss: () -> Unit,
    )
}


val LocalScreenStateConfig = compositionLocalOf {
    screenStateConfig()
}

fun screenStateConfig(
    logo: DrawableResource? = null,
    infoDialog: @Composable (
        infoMessage: InfoMessage,
        onDismiss: () -> Unit
    ) -> Unit = { infoMessage, onDismiss ->
        InfoDialog(
            message = infoMessage,
            onDismiss = onDismiss
        )
    },
    progressDialog: @Composable (
        progress: Float,
        onDismissProgress: (() -> Unit)?
    ) -> Unit = { progress, onDismissProgress ->
        ProgressDialog(
            progress = progress,
            logo = logo?.let { painterResource(it) },
            onDismissProgress = onDismissProgress
        )
    }
) = object : ScreenStateConfig {
    override val logo: DrawableResource? = logo

    @Composable
    override fun ProgressDialog(
        progress: Float,
        onDismissProgress: (() -> Unit)?
    ) = progressDialog(progress, onDismissProgress)

    @Composable
    override fun InfoDialog(
        infoMessage: InfoMessage,
        onDismiss: () -> Unit
    ) = infoDialog(infoMessage, onDismiss)
}

@Composable
fun ScreenStateWrapper(
    config: ScreenStateConfig = LocalScreenStateConfig.current,
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
