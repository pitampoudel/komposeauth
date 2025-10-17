package com.vardansoft.core.presentation.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EnumField(
    value: T,
    onValueChange: (T) -> Unit,
    asString: @Composable T.() -> String?,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    prependOptions: List<String> = listOf(),
    appendOptions: List<String> = listOf(),
    onExtraOptionClick: (String) -> Unit = {},
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    options: Iterable<T>,
    supportingText: (@Composable () -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = {
            expanded = if (enabled) !expanded else false
        }
    ) {
        OutlinedTextField(
            enabled = enabled,
            modifier = textFieldModifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
            readOnly = true,
            value = value.asString().orEmpty(),
            onValueChange = { },
            label = label,
            placeholder = placeholder,
            isError = isError,
            leadingIcon = leadingIcon,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            supportingText = supportingText
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            prependOptions.forEach {
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onExtraOptionClick(it)
                    },
                    text = {
                        Text(it)
                    }
                )
            }
            options.forEach {
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onValueChange(it)
                    },
                    text = {
                        Text(it.asString().orEmpty())
                    }
                )
            }
            appendOptions.forEach {
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onExtraOptionClick(it)
                    },
                    text = {
                        Text(it)
                    }
                )
            }
        }
    }
}