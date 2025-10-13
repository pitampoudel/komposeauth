package com.vardansoft.authx.ui.core.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import com.vardansoft.core.domain.asDisplayDate
import com.vardansoft.core.domain.asDisplayDateTime
import com.vardansoft.core.domain.asDisplayTime
import com.vardansoft.core.domain.toInstant
import com.vardansoft.core.domain.toSystemLocalDateTime
import com.vardansoft.ui.generated.resources.Res
import com.vardansoft.ui.generated.resources.cancel
import com.vardansoft.ui.generated.resources.ok
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


enum class DateTimeFieldType {
    DATE,
    TIME,
    DATE_AND_TIME
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DateTimeField(
    type: DateTimeFieldType,
    value: Instant?,
    onValueChange: (Instant?) -> Unit,
    field: @Composable (text: String, onClick: () -> Unit) -> Unit
) {
    val valueAsLocalDateTime = remember(value) {
        value?.toSystemLocalDateTime()
    }
    var isShowingDatePickerDialog by remember {
        mutableStateOf(false)
    }
    var isShowingTimePickerDialog by remember {
        mutableStateOf(false)
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = value?.toEpochMilliseconds()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = valueAsLocalDateTime?.hour ?: 0,
        initialMinute = valueAsLocalDateTime?.minute ?: 0,
        is24Hour = false
    )
    if (isShowingDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = {
                isShowingDatePickerDialog = false
            },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(datePickerState.selectedDateMillis?.let {
                        Instant.fromEpochMilliseconds(it)
                    })
                    isShowingDatePickerDialog = false
                    if (type == DateTimeFieldType.DATE_AND_TIME) {
                        isShowingTimePickerDialog = true
                    }
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isShowingDatePickerDialog = false
                }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
        {
            DatePicker(datePickerState)
        }
    }
    if (isShowingTimePickerDialog) {
        AlertDialog(
            onDismissRequest = {
                isShowingTimePickerDialog = false
            },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(
                        valueAsLocalDateTime?.date?.let { date ->
                            LocalDateTime(
                                date = date,
                                time = LocalTime(timePickerState.hour, timePickerState.minute)
                            ).toInstant()
                        }
                    )
                    isShowingTimePickerDialog = false
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isShowingTimePickerDialog = false
                }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            text = {
                TimePicker(timePickerState)
            }
        )

    }
    field(
        when (type) {
            DateTimeFieldType.DATE -> valueAsLocalDateTime?.date?.asDisplayDate().orEmpty()
            DateTimeFieldType.TIME -> valueAsLocalDateTime?.time?.asDisplayTime().orEmpty()
            DateTimeFieldType.DATE_AND_TIME -> valueAsLocalDateTime?.asDisplayDateTime()
                .orEmpty()
        }
    ) {
        when (type) {
            DateTimeFieldType.DATE -> isShowingDatePickerDialog = true
            DateTimeFieldType.TIME -> isShowingTimePickerDialog = true
            DateTimeFieldType.DATE_AND_TIME -> isShowingDatePickerDialog = true
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DateTimeField(
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    label: (@Composable () -> Unit)? = null,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    shape: Shape = OutlinedTextFieldDefaults.shape,
    type: DateTimeFieldType,
    value: Instant?,
    onValueChange: (Instant?) -> Unit,
) {
    DateTimeField(
        type = type,
        value = value,
        onValueChange = onValueChange
    ) { text, onClick ->
        OutlinedTextField(
            modifier = modifier.onFocusChanged {
                if (it.hasFocus) {
                    onClick()
                }
            },
            colors = colors,
            shape = shape,
            enabled = enabled,
            isError = isError,
            value = text,
            onValueChange = {},
            readOnly = true,
            label = label,
            supportingText = supportingText,
            trailingIcon = {
                IconButton(onClick = onClick) {
                    Icon(Icons.Default.CalendarToday, null)
                }
            },
            leadingIcon = leadingIcon
        )

    }


}