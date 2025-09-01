package com.vardansoft.authx.ui.kyc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.vardansoft.authx.ui.core.wrapper.screenstate.ScreenStateWrapper
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
                            Text("Skip")
                        }
                    },

                    title = { Text("KYC Verification") }
                )
            }
        ) { innerPadding ->
            val enabled = !state.isApproved && !state.isPending
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Provide your details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "We securely verify your identity to keep your account safe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                state.existing?.let { existing ->
                    Column(
                        Modifier.fillMaxWidth().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Current status: ${existing.status}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        existing.remarks?.let {
                            Text(
                                "Remarks: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        placeholder = { Text("e.g., Passport, Driver License") },
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
                        placeholder = { Text("Enter document number") },
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
                        placeholder = { Text("Issuing country") },
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
                    OutlinedTextField(
                        value = state.documentFrontUrl,
                        onValueChange = { onEvent(KycEvent.DocumentFrontUrlChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Document front URL (optional)") },
                        placeholder = { Text("Link to front image") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.documentBackUrl,
                        onValueChange = { onEvent(KycEvent.DocumentBackUrlChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Document back URL (optional)") },
                        placeholder = { Text("Link to back image") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.selfieUrl,
                        onValueChange = { onEvent(KycEvent.SelfieUrlChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Selfie URL (optional)") },
                        placeholder = { Text("Link to a selfie for verification") },
                        singleLine = true
                    )
                }

                Button(
                    onClick = { onEvent(KycEvent.Submit) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (enabled) "Submit for Review" else "Submission in progress or completed")
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

