package com.vardansoft.authx.ui.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vardansoft.authx.data.UpdatePhoneNumberRequest
import com.vardansoft.authx.domain.use_cases.ValidateOtpCode.Companion.OTP_LENGTH
import com.vardansoft.authx.ui.core.components.CountryPicker
import com.vardansoft.authx.ui.core.wrapper.screenstate.ScreenStateWrapper
import org.koin.compose.viewmodel.koinViewModel
import kotlin.reflect.KFunction1
import com.vardansoft.ui.generated.resources.Res
import com.vardansoft.ui.generated.resources.common_skip
import com.vardansoft.ui.generated.resources.otp_enter_mobile_title
import com.vardansoft.ui.generated.resources.otp_fill_code_title
import com.vardansoft.ui.generated.resources.otp_phone_label
import com.vardansoft.ui.generated.resources.otp_phone_placeholder
import com.vardansoft.ui.generated.resources.otp_send_button
import com.vardansoft.ui.generated.resources.otp_sent_to
import com.vardansoft.ui.generated.resources.otp_verify_button
import org.jetbrains.compose.resources.stringResource

@Composable
fun AuthXOtpScreen(
    onSkip: () -> Unit = {}
) {
    val vm = koinViewModel<OtpViewModel>()
    val state = vm.state.collectAsState().value
    AuthXOtpPage(
        state = state,
        onEvent = vm::onEvent,
        uiEvents = vm.uiEvents,
        onSkip = onSkip
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthXOtpPage(
    state: OtpState,
    uiEvents: kotlinx.coroutines.flow.Flow<OtpUiEvent>,
    onEvent: KFunction1<OtpEvent, Unit>,
    onSkip: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        uiEvents.collect {
            when (it) {
                is OtpUiEvent.Verified -> onSkip()
            }
        }
    }
    ScreenStateWrapper(
        progress = state.progress,
        infoMessage = state.infoMsg,
        onDismissInfoMsg = { onEvent(OtpEvent.DismissInfoMsg) }
    ) {
        Scaffold { paddingValues ->
            if (state.req == null) {
                return@Scaffold EnterMobilePage(
                    paddingValues = paddingValues,
                    onEvent = onEvent,
                    onSkip = onSkip
                )
            }
            FillTheCodePage(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                state = state,
                onEvent = onEvent,
                onSkip = onSkip
            )
        }
    }
}


@Composable
private fun EnterMobilePage(
    paddingValues: PaddingValues,
    onEvent: (OtpEvent) -> Unit,
    onSkip: () -> Unit = {}
) {
    var phoneNumber by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(
                text = stringResource(Res.string.otp_enter_mobile_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.otp_phone_label)) },
                placeholder = { Text(stringResource(Res.string.otp_phone_placeholder)) },
                leadingIcon = {
                    CountryPicker(
                        countryNameCode = countryCode,
                        onCountryNameCodeChanged = { countryCode = it }
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    onEvent(
                        OtpEvent.SubmitPhoneNumber(
                            UpdatePhoneNumberRequest(
                                phoneNumber = phoneNumber,
                                countryCode = countryCode
                            )
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = phoneNumber.length >= 6
            ) {
                Text(stringResource(Res.string.otp_send_button))
            }
            TextButton(onClick = { onSkip() }) {
                Text(stringResource(Res.string.common_skip))
            }
        }
    }
}


@Composable
private fun FillTheCodePage(
    modifier: Modifier = Modifier,
    state: OtpState,
    onEvent: KFunction1<OtpEvent, Unit>,
    onSkip: () -> Unit = {}
) {
    val isRegistered = registerSmsOtpRetriever {
        if (it.length == OTP_LENGTH) {
            onEvent(OtpEvent.CodeChanged(it))
        }
    }

    LaunchedEffect(isRegistered) {
        if (isRegistered != null) {
            state.req?.let {
                onEvent(OtpEvent.SendOtp)
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(
                text = stringResource(Res.string.otp_fill_code_title),
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = stringResource(Res.string.otp_sent_to, state.req?.phoneNumber ?: ""),
                style = MaterialTheme.typography.bodyLarge
            )

            OTPTextField(state.code) { onEvent(OtpEvent.CodeChanged(it)) }

        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onEvent(OtpEvent.Verify) },
                enabled = !state.containsError(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(stringResource(Res.string.otp_verify_button))
            }
            TextButton(onClick = { onSkip() }) {
                Text(stringResource(Res.string.common_skip))
            }
        }
    }
}


@Composable
fun OTPTextField(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = {
            if (it.length <= OTP_LENGTH) onValueChange(it)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    ) { innerTextField ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(OTP_LENGTH) { index ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.getOrNull(index)?.toString() ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}