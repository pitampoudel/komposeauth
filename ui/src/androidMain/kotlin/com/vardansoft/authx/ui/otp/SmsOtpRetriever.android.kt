package com.vardansoft.authx.ui.otp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.auth.api.phone.SmsRetrieverClient
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Task
import com.vardansoft.authx.domain.use_cases.ValidateOtpCode.OTP_LENGTH
import java.util.regex.Matcher
import java.util.regex.Pattern


private fun extractOtpFromSms(message: String): String? {
    val pattern: Pattern = Pattern.compile("(|^)\\d{$OTP_LENGTH}")
    val matcher: Matcher = pattern.matcher(message)
    return if (matcher.find()) matcher.group(0) else null
}

@Composable
actual fun registerSmsOtpRetriever(onRetrieved: (String) -> Unit): Boolean? {
    var isRegistered: Boolean? by remember { mutableStateOf(null) }
    val ctx = LocalContext.current
    val smsContestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Obtain the phone number from the result
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            result.data?.let {
                val message = it.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                message?.let { txt ->
                    extractOtpFromSms(txt)?.let { otp ->
                        onRetrieved(otp)
                    }
                }
            }
        } else {
            // Consent denied. User can type OTP manually.
        }
    }
    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val isTiramisuOrUp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
                    val extras = intent.extras

                    val status: Status? = extras?.get(SmsRetriever.EXTRA_STATUS) as Status?
                    if (status != null) {
                        when (status.statusCode) {
                            CommonStatusCodes.SUCCESS -> {
                                val consentIntent = if (isTiramisuOrUp) {
                                    extras.getParcelable(
                                        SmsRetriever.EXTRA_CONSENT_INTENT,
                                        Intent::class.java
                                    )
                                } else {
                                    extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT)
                                }
                                try {
                                    if (consentIntent != null) {
                                        smsContestLauncher.launch(consentIntent)
                                    }
                                } catch (e: ActivityNotFoundException) {
                                    // Handle the exception ...
                                }
                            }

                            CommonStatusCodes.TIMEOUT -> {
                            }
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(Unit) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(
                receiver,
                IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
                SmsRetriever.SEND_PERMISSION,
                null,
                Context.RECEIVER_EXPORTED
            )
        }

        val client: SmsRetrieverClient = SmsRetriever.getClient(ctx)
        val task: Task<Void> = client.startSmsUserConsent(null)

        task.addOnSuccessListener {
            isRegistered = true
        }

        task.addOnFailureListener {
            isRegistered = false
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.unregisterReceiver(receiver)
            }
        }
    }
    return isRegistered
}

