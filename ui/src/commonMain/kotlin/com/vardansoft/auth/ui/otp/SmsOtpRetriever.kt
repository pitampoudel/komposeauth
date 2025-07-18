package com.vardansoft.auth.ui.otp

import androidx.compose.runtime.Composable


@Composable
expect fun registerSmsOtpRetriever(onRetrieved: (String) -> Unit): Boolean?