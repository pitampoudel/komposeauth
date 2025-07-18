package com.vardansoft.auth.ui.core.wrapper.screenstate

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable


@Composable
fun ScreenStateWrapper(
    progress: Float?,
    infoMessage: String?,
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
