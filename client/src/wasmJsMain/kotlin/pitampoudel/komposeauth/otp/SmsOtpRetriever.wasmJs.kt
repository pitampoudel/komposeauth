package pitampoudel.komposeauth.otp

import androidx.compose.runtime.Composable

@Composable
actual fun registerSmsOtpRetriever(onRetrieved: (String) -> Unit): Boolean? {
    return false
}