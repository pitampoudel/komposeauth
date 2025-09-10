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
import com.vardansoft.authx.data.DocumentType
import com.vardansoft.authx.data.KycResponse
import com.vardansoft.authx.ui.core.components.DateTimeField
import com.vardansoft.authx.ui.core.components.DateTimeFieldType
import com.vardansoft.authx.ui.core.components.EnumField
import com.vardansoft.authx.ui.core.components.FileInputField
import com.vardansoft.authx.ui.core.toSystemLocalDate
import com.vardansoft.authx.ui.core.wrapper.screenstate.ScreenStateWrapper
import com.vardansoft.ui.generated.resources.Res
import com.vardansoft.ui.generated.resources.common_skip
import com.vardansoft.ui.generated.resources.kyc_document_back_hint
import com.vardansoft.ui.generated.resources.kyc_document_back_title
import com.vardansoft.ui.generated.resources.kyc_document_details_section_title
import com.vardansoft.ui.generated.resources.kyc_document_expiry_date_label
import com.vardansoft.ui.generated.resources.kyc_document_front_hint
import com.vardansoft.ui.generated.resources.kyc_document_front_title
import com.vardansoft.ui.generated.resources.kyc_document_issue_date_label
import com.vardansoft.ui.generated.resources.kyc_document_issued_place_label
import com.vardansoft.ui.generated.resources.kyc_document_issued_place_placeholder
import com.vardansoft.ui.generated.resources.kyc_document_number_label
import com.vardansoft.ui.generated.resources.kyc_document_number_placeholder
import com.vardansoft.ui.generated.resources.kyc_document_type_label
import com.vardansoft.ui.generated.resources.kyc_document_type_placeholder
import com.vardansoft.ui.generated.resources.kyc_documents_section_title
import com.vardansoft.ui.generated.resources.kyc_nationality_label
import com.vardansoft.ui.generated.resources.kyc_nationality_placeholder
import com.vardansoft.ui.generated.resources.kyc_personal_info_section_title
import com.vardansoft.ui.generated.resources.kyc_provide_details
import com.vardansoft.ui.generated.resources.kyc_selfie_hint
import com.vardansoft.ui.generated.resources.kyc_selfie_title
import com.vardansoft.ui.generated.resources.kyc_submit_action
import com.vardansoft.ui.generated.resources.kyc_submit_approved
import com.vardansoft.ui.generated.resources.kyc_submit_pending
import com.vardansoft.ui.generated.resources.kyc_submit_progress
import com.vardansoft.ui.generated.resources.kyc_subtitle
import com.vardansoft.ui.generated.resources.kyc_title
import com.vardansoft.ui.generated.resources.kyc_first_name_label
import com.vardansoft.ui.generated.resources.kyc_first_name_placeholder
import com.vardansoft.ui.generated.resources.kyc_middle_name_label
import com.vardansoft.ui.generated.resources.kyc_middle_name_placeholder
import com.vardansoft.ui.generated.resources.kyc_last_name_label
import com.vardansoft.ui.generated.resources.kyc_last_name_placeholder
import com.vardansoft.ui.generated.resources.kyc_date_of_birth_label
import com.vardansoft.ui.generated.resources.kyc_gender_label
import com.vardansoft.ui.generated.resources.kyc_gender_placeholder
import com.vardansoft.ui.generated.resources.kyc_family_details_title
import com.vardansoft.ui.generated.resources.kyc_father_name_label
import com.vardansoft.ui.generated.resources.kyc_father_name_placeholder
import com.vardansoft.ui.generated.resources.kyc_mother_name_label
import com.vardansoft.ui.generated.resources.kyc_mother_name_placeholder
import com.vardansoft.ui.generated.resources.kyc_marital_status_label
import com.vardansoft.ui.generated.resources.kyc_marital_status_placeholder
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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

                    title = {
                        Text(stringResource(Res.string.kyc_title))
                    }
                )
            }
        ) { innerPadding ->
            val enabled = !state.isApproved && !state.isPending && state.progress == null
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
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
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.kyc_personal_info_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = state.nationality,
                        onValueChange = { onEvent(KycEvent.NationalityChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.kyc_nationality_label)) },
                        placeholder = { Text(stringResource(Res.string.kyc_nationality_placeholder)) },
                        isError = state.nationalityError != null,
                        singleLine = true,
                        supportingText = {
                            state.nationalityError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = state.firstName,
                        onValueChange = { onEvent(KycEvent.FirstNameChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.kyc_first_name_label)) },
                        placeholder = { Text(stringResource(Res.string.kyc_first_name_placeholder)) },
                        isError = state.firstNameError != null,
                        singleLine = true,
                        supportingText = {
                            state.firstNameError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = state.middleName,
                        onValueChange = { onEvent(KycEvent.MiddleNameChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.kyc_middle_name_label)) },
                        placeholder = { Text(stringResource(Res.string.kyc_middle_name_placeholder)) },
                        isError = state.middleNameError != null,
                        singleLine = true,
                        supportingText = {
                            state.middleNameError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = state.lastName,
                        onValueChange = { onEvent(KycEvent.LastNameChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.kyc_last_name_label)) },
                        placeholder = { Text(stringResource(Res.string.kyc_last_name_placeholder)) },
                        isError = state.lastNameError != null,
                        singleLine = true,
                        supportingText = {
                            state.lastNameError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    DateTimeField(
                        value = state.dateOfBirth?.atStartOfDayIn(TimeZone.currentSystemDefault()),
                        onValueChange = { instant ->
                            onEvent(KycEvent.DateOfBirthChanged(instant?.toSystemLocalDate()))
                        },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.kyc_date_of_birth_label)) },
                        type = DateTimeFieldType.DATE,
                        isError = state.dateOfBirthError != null,
                        supportingText = {
                            state.dateOfBirthError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    EnumField(
                        options = KycResponse.Gender.entries,
                        asString = {
                            this?.toStringRes()?.let {
                                stringResource(it)
                            }
                        },
                        value = state.gender,
                        onValueChange = { onEvent(KycEvent.GenderChanged(it)) },
                        enabled = enabled,
                        textFieldModifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.kyc_gender_label)) },
                        placeholder = { Text(stringResource(Res.string.kyc_gender_placeholder)) },
                        isError = state.genderError != null,
                        supportingText = {
                            state.genderError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.kyc_family_details_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = state.fatherName,
                        onValueChange = { onEvent(KycEvent.FatherNameChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.kyc_father_name_label)) },
                        placeholder = { Text(stringResource(Res.string.kyc_father_name_placeholder)) },
                        isError = state.fatherNameError != null,
                        singleLine = true,
                        supportingText = {
                            state.fatherNameError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = state.motherName,
                        onValueChange = { onEvent(KycEvent.MotherNameChanged(it)) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.kyc_mother_name_label)) },
                        placeholder = { Text(stringResource(Res.string.kyc_mother_name_placeholder)) },
                        isError = state.motherNameError != null,
                        singleLine = true,
                        supportingText = {
                            state.motherNameError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    EnumField(
                        options = KycResponse.MaritalStatus.entries,
                        asString = {
                            this?.toStringRes()?.let {
                                stringResource(it)
                            }
                        },
                        value = state.maritalStatus,
                        onValueChange = { onEvent(KycEvent.MaritalStatusChanged(it)) },
                        enabled = enabled,
                        textFieldModifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.kyc_marital_status_label)) },
                        placeholder = { Text(stringResource(Res.string.kyc_marital_status_placeholder)) },
                        isError = state.maritalStatusError != null,
                        supportingText = {
                            state.maritalStatusError?.let { err ->
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.kyc_document_details_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        EnumField(
                            value = state.documentType,
                            asString = {
                                this?.let { stringResource(it.toStringRes()) }
                            },
                            onValueChange = { documentType ->
                                onEvent(KycEvent.DocumentTypeChanged(documentType))
                            },
                            enabled = enabled,
                            textFieldModifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(Res.string.kyc_document_type_label)) },
                            placeholder = { Text(stringResource(Res.string.kyc_document_type_placeholder)) },
                            isError = state.documentTypeError != null,
                            options = DocumentType.entries,
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
                            label = { Text(stringResource(Res.string.kyc_document_number_label)) },
                            placeholder = { Text(stringResource(Res.string.kyc_document_number_placeholder)) },
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

                        DateTimeField(
                            value = state.documentIssuedDate?.atStartOfDayIn(TimeZone.currentSystemDefault()),
                            onValueChange = { instant ->
                                onEvent(
                                    KycEvent.DocumentIssuedDateChanged(
                                        instant?.toSystemLocalDate()
                                    )
                                )
                            },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(Res.string.kyc_document_issue_date_label)) },
                            type = DateTimeFieldType.DATE,
                            isError = state.documentIssuedDateError != null,
                            supportingText = {
                                state.documentIssuedDateError?.let { err ->
                                    Text(
                                        text = err,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        )

                        DateTimeField(
                            value = state.documentExpiryDate?.atStartOfDayIn(TimeZone.currentSystemDefault()),
                            onValueChange = { instant ->
                                onEvent(
                                    KycEvent.DocumentExpiryDateChanged(
                                        instant?.toSystemLocalDate()
                                    )
                                )
                            },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(Res.string.kyc_document_expiry_date_label)) },
                            type = DateTimeFieldType.DATE,
                            isError = state.documentExpiryDateError != null,
                            supportingText = {
                                state.documentExpiryDateError?.let { err ->
                                    Text(
                                        text = err,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        )

                        OutlinedTextField(
                            value = state.documentIssuedPlace,
                            onValueChange = { onEvent(KycEvent.DocumentIssuedPlaceChanged(it)) },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(Res.string.kyc_document_issued_place_label)) },
                            placeholder = { Text(stringResource(Res.string.kyc_document_issued_place_placeholder)) },
                            isError = state.documentIssuedPlaceError != null,
                            singleLine = true,
                            supportingText = {
                                state.documentIssuedPlaceError?.let { err ->
                                    Text(
                                        text = err,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.kyc_documents_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        FileInputField(
                            title = stringResource(Res.string.kyc_document_front_title),
                            hint = stringResource(Res.string.kyc_document_front_hint),
                            mimeType = "image/*",
                            file = state.documentFront,
                            enabled = enabled,
                            onSelected = {
                                onEvent(KycEvent.DocumentFrontSelected(it))
                            }
                        )

                        FileInputField(
                            title = stringResource(Res.string.kyc_document_back_title),
                            hint = stringResource(Res.string.kyc_document_back_hint),
                            mimeType = "image/*",
                            file = state.documentBack,
                            enabled = enabled,
                            onSelected = {
                                onEvent(KycEvent.DocumentBackSelected(it))
                            }
                        )

                        FileInputField(
                            title = stringResource(Res.string.kyc_selfie_title),
                            hint = stringResource(Res.string.kyc_selfie_hint),
                            mimeType = "image/*",
                            file = state.selfie,
                            enabled = enabled,
                            onSelected = {
                                onEvent(KycEvent.SelfieSelected(it))
                            }
                        )
                    }

                    Button(
                        onClick = { onEvent(KycEvent.Submit) },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val label = when {
                            state.progress != null -> stringResource(Res.string.kyc_submit_progress)
                            state.isPending -> stringResource(Res.string.kyc_submit_pending)
                            state.isApproved -> stringResource(Res.string.kyc_submit_approved)
                            else -> stringResource(Res.string.kyc_submit_action)
                        }
                        Text(label)
                    }

                }
            }
        }
    }
}
