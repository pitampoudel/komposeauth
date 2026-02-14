package pitampoudel.core.presentation.wrapper.screenstate

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Dialog

@Composable
fun ProgressDialog(progress: Float, logo: Painter?, onDismissProgress: (() -> Unit)?) {
    Dialog(onDismissRequest = { onDismissProgress?.invoke() }) {
        if (progress != 0F) {
            CircularProgressIndicator(
                progress = {
                    progress
                }
            )
        } else {
            logo?.let {
                LogoLoadingAnimation(logo = logo)
            } ?: run {
                LoadingAnimation()
            }
        }
    }
}
