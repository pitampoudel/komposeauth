package com.vardansoft.auth.ui.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vardansoft.auth.data.UpdatePhoneNumberRequest
import com.vardansoft.auth.domain.use_cases.ValidateOtpCode.Companion.OTP_LENGTH
import com.vardansoft.auth.presentation.otp.OtpEvent
import com.vardansoft.auth.presentation.otp.OtpState
import com.vardansoft.auth.presentation.otp.OtpUiEvent
import com.vardansoft.auth.ui.core.components.CountryPicker
import com.vardansoft.auth.ui.core.wrapper.screenstate.ScreenStateWrapper
import kotlin.reflect.KFunction1


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    state: OtpState,
    uiEvents: kotlinx.coroutines.flow.Flow<OtpUiEvent>,
    onEvent: KFunction1<OtpEvent, Unit>,
    popBackStack: () -> Unit
) {
    LaunchedEffect(Unit) {
        uiEvents.collect {
            when (it) {
                is OtpUiEvent.Verified -> popBackStack()
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
                return@Scaffold EnterMobilePage(paddingValues, onEvent)
            }
            FillTheCodePage(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                state = state,
                onEvent = onEvent
            )
        }
    }
}


@Composable
fun EnterMobilePage(
    paddingValues: PaddingValues,
    onEvent: (OtpEvent) -> Unit
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
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                text = "Enter your mobile number",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone Number") },
                placeholder = { Text("e.g. 9812345678") },
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
            Text("Send OTP")
        }
    }
}


@Composable
fun FillTheCodePage(
    modifier: Modifier = Modifier,
    state: OtpState,
    onEvent: KFunction1<OtpEvent, Unit>
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Fill The Code",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            textAlign = TextAlign.Center,
            text = "An otp code has just been sent to ${state.req?.phoneNumber}",
            style = MaterialTheme.typography.titleMedium
        )

        OTPTextField(state.code, onValueChange = { onEvent(OtpEvent.CodeChanged(it)) })

        Button(
            enabled = !state.containsError(),
            onClick = { onEvent(OtpEvent.Verify) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Verify")
        }


    }
}

@Composable
fun OTPTextField(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = {
            if (it.length <= OTP_LENGTH) {
                onValueChange(it)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword
        )
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(OTP_LENGTH) { index ->
                Box(
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ).padding(5.dp).size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            index >= value.length -> ""
                            else -> value[index].toString()
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

            }
        }
    }
}
