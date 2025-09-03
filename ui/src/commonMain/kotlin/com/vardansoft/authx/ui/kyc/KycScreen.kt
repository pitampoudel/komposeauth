package com.vardansoft.authx.ui.kyc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vardansoft.authx.ui.core.components.rememberFilePicker
import com.vardansoft.authx.ui.core.wrapper.screenstate.ScreenStateWrapper
import com.vardansoft.ui.generated.resources.Res
import com.vardansoft.ui.generated.resources.common_skip
import com.vardansoft.ui.generated.resources.kyc_current_status
import com.vardansoft.ui.generated.resources.kyc_provide_details
import com.vardansoft.ui.generated.resources.kyc_remarks
import com.vardansoft.ui.generated.resources.kyc_subtitle
import com.vardansoft.ui.generated.resources.kyc_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun KycScreen(
    onSkip: () -> Unit = {}
) {
    val vm = koinViewModel<KycViewModel>()
    val state by vm.state.collectAsState()
    KycPage(
        state = state,
        onEvent = vm::onEvent,
        onSkip = onSkip
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KycPage(
    state: KycState,
    onEvent: (KycEvent) -> Unit,
    onSkip: () -> Unit
) {
    LaunchedEffect(Unit) {
        onEvent(KycEvent.LoadExisting)
    }

    ScreenStateWrapper(
        progress = state.progress,
        infoMessage = state.infoMsg,
        onDismissInfoMsg = { onEvent(KycEvent.DismissInfoMsg) }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    actions = {
                        TextButton(onClick = onSkip) {
                            Text(stringResource(Res.string.common_skip))
                        }
                    },

                    title = { Text(stringResource(Res.string.kyc_title)) }
                )
            }
        ) { innerPadding ->
            val enabled = !state.isApproved && !state.isPending && state.progress == null
            val onPickFront = rememberFilePicker("image/*,application/pdf") {
                onEvent(
                    KycEvent.DocumentFrontSelected(it)
                )
            }
            val onPickBack = rememberFilePicker("image/*,application/pdf") {
                onEvent(
                    KycEvent.DocumentBackSelected(it)
                )
            }
            val onPickSelfie = rememberFilePicker("image/*") {
                onEvent(KycEvent.SelfieSelected(it))
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.kyc_provide_details),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(Res.string.kyc_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                state.existing?.let { existing ->
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(Res.string.kyc_current_status, existing.status),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        existing.remarks?.let {
                            Text(
                                stringResource(Res.string.kyc_remarks, it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = state.fullName,
                        onValueChange = { onEvent(KycEvent.FullNameChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Full name") },
                        placeholder = { Text("Enter your full legal name") },
                        isError = state.fullNameError != null,
                        singleLine = true,
                        supportingText = {
                            state.fullNameError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = state.documentType,
                        onValueChange = { onEvent(KycEvent.DocumentTypeChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Document type") },
                        placeholder = { Text("E.g., passport, driver license") },
                        isError = state.documentTypeError != null,
                        singleLine = true,
                        supportingText = {
                            state.documentTypeError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = state.documentNumber,
                        onValueChange = { onEvent(KycEvent.DocumentNumberChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Document number") },
                        placeholder = { Text("Enter your document number") },
                        isError = state.documentNumberError != null,
                        singleLine = true,
                        supportingText = {
                            state.documentNumberError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = state.country,
                        onValueChange = { onEvent(KycEvent.CountryChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Country") },
                        placeholder = { Text("Enter issuing country") },
                        isError = state.countryError != null,
                        singleLine = true,
                        supportingText = {
                            state.countryError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Documents",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        FileInput(
                            title = "Document front",
                            hint = "Upload the front side (JPG, PNG, PDF)",
                            hasFile = state.documentFront != null,
                            enabled = enabled,
                            onClick = { onPickFront.launch() }
                        )

                        FileInput(
                            title = "Document back",
                            hint = "Upload the back side (JPG, PNG, PDF)",
                            hasFile = state.documentBack != null,
                            enabled = enabled,
                            onClick = { onPickBack.launch() }
                        )

                        FileInput(
                            title = "Selfie",
                            hint = "Upload a selfie for verification",
                            hasFile = state.selfie != null,
                            enabled = enabled,
                            onClick = { onPickSelfie.launch() }
                        )
                    }
                }

                Button(
                    onClick = { onEvent(KycEvent.Submit) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val label = when {
                        state.progress != null -> "Submitting..."
                        state.isPending -> "Submission pending review"
                        state.isApproved -> "KYC approved"
                        else -> "Submit for review"
                    }
                    Text(label)
                }

            }
        }
    }
}


@Composable
private fun FileInput(
    title: String,
    hint: String,
    hasFile: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (hasFile) "File selected" else "No file selected",
            style = MaterialTheme.typography.bodySmall,
            color = if (hasFile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onClick, enabled = enabled) {
            Text(if (hasFile) "Change file" else "Choose file")
        }
    }
}
