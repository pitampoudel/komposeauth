package com.vardansoft.komposeauth.otp

import androidx.compose.runtime.Composable


@Composable
expect fun registerSmsOtpRetriever(onRetrieved: (String) -> Unit): Boolean?