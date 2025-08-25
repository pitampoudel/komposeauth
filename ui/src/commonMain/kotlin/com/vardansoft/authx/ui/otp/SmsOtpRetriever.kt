package com.vardansoft.authx.ui.otp

import androidx.compose.runtime.Composable


@Composable
internal expect fun registerSmsOtpRetriever(onRetrieved: (String) -> Unit): Boolean?