package com.vardansoft.authx.ui.core.wrapper.screenstate

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.vardansoft.core.presentation.InfoMessage


@Composable
fun ScreenStateWrapper(
    progress: Float?,
    infoMessage: InfoMessage?,
    onDismissInfoMsg: () -> Unit,
    content: @Composable () -> Unit
) {
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
