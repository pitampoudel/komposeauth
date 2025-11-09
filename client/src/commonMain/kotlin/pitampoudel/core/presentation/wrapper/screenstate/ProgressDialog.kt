package pitampoudel.core.presentation.wrapper.screenstate

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Dialog

@Composable
fun ProgressDialog(progress: Float, logo: Painter?) {
    if (progress != 0F) {
        Dialog(onDismissRequest = { }) {
        }
        LinearProgressIndicator(
            progress = {
                progress
            },
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Dialog(onDismissRequest = { }) {
            logo?.let {
                LogoLoadingAnimation(logo = logo)
            } ?: run {
                LoadingAnimation()
            }
        }
    }
}

