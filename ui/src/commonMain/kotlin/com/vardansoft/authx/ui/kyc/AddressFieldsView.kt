package com.vardansoft.authx.ui.kyc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vardansoft.ui.generated.resources.Res
import com.vardansoft.ui.generated.resources.kyc_address_country_label
import com.vardansoft.ui.generated.resources.kyc_address_country_placeholder
import com.vardansoft.ui.generated.resources.kyc_address_district_label
import com.vardansoft.ui.generated.resources.kyc_address_district_placeholder
import com.vardansoft.ui.generated.resources.kyc_address_municipality_label
import com.vardansoft.ui.generated.resources.kyc_address_municipality_placeholder
import com.vardansoft.ui.generated.resources.kyc_address_province_label
import com.vardansoft.ui.generated.resources.kyc_address_province_placeholder
import com.vardansoft.ui.generated.resources.kyc_address_tole_label
import com.vardansoft.ui.generated.resources.kyc_address_tole_placeholder
import com.vardansoft.ui.generated.resources.kyc_address_ward_no_label
import com.vardansoft.ui.generated.resources.kyc_address_ward_no_placeholder
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddressFieldsView(
    currentValue: AddressState,
    onEvent: (KycEvent) -> Unit,
    enabled: Boolean,
    addressType: AddressType
) {
    Column {
        OutlinedTextField(
            value = currentValue.country,
            onValueChange = {
                onEvent(
                    when (addressType) {
                        AddressType.CURRENT -> KycEvent.CurrentAddressCountryChanged(it)
                        AddressType.PERMANENT -> KycEvent.PermanentAddressCountryChanged(it)
                    }
                )
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.kyc_address_country_label)) },
            placeholder = { Text(stringResource(Res.string.kyc_address_country_placeholder)) },
            isError = currentValue.countryError != null,
            singleLine = true,
            supportingText = {
                currentValue.countryError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        OutlinedTextField(
            value = currentValue.province,
            onValueChange = {
                onEvent(
                    when (addressType) {
                        AddressType.CURRENT -> KycEvent.CurrentAddressProvinceChanged(it)
                        AddressType.PERMANENT -> KycEvent.PermanentAddressProvinceChanged(it)
                    }
                )
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.kyc_address_province_label)) },
            placeholder = { Text(stringResource(Res.string.kyc_address_province_placeholder)) },
            isError = currentValue.provinceError != null,
            singleLine = true,
            supportingText = {
                currentValue.provinceError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        OutlinedTextField(
            value = currentValue.district,
            onValueChange = {
                onEvent(
                    when (addressType) {
                        AddressType.CURRENT -> KycEvent.CurrentAddressDistrictChanged(it)
                        AddressType.PERMANENT -> KycEvent.PermanentAddressDistrictChanged(it)
                    }
                )
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.kyc_address_district_label)) },
            placeholder = { Text(stringResource(Res.string.kyc_address_district_placeholder)) },
            isError = currentValue.districtError != null,
            singleLine = true,
            supportingText = {
                currentValue.districtError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        OutlinedTextField(
            value = currentValue.localUnit,
            onValueChange = {
                onEvent(
                    when (addressType) {
                        AddressType.CURRENT -> KycEvent.CurrentAddressLocalUnitChanged(it)
                        AddressType.PERMANENT -> KycEvent.PermanentAddressLocalUnitChanged(it)
                    }
                )
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.kyc_address_municipality_label)) },
            placeholder = { Text(stringResource(Res.string.kyc_address_municipality_placeholder)) },
            isError = currentValue.localUnitError != null,
            singleLine = true,
            supportingText = {
                currentValue.localUnitError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        OutlinedTextField(
            value = currentValue.wardNo,
            onValueChange = {
                onEvent(
                    when (addressType) {
                        AddressType.CURRENT -> KycEvent.CurrentAddressWardNoChanged(it)
                        AddressType.PERMANENT -> KycEvent.PermanentAddressWardNoChanged(it)
                    }
                )
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.kyc_address_ward_no_label)) },
            placeholder = { Text(stringResource(Res.string.kyc_address_ward_no_placeholder)) },
            isError = currentValue.wardNoError != null,
            singleLine = true,
            supportingText = {
                currentValue.wardNoError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        OutlinedTextField(
            value = currentValue.tole,
            onValueChange = {
                onEvent(
                    when (addressType) {
                        AddressType.CURRENT -> KycEvent.CurrentAddressToleChanged(it)
                        AddressType.PERMANENT -> KycEvent.PermanentAddressToleChanged(it)
                    }
                )
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.kyc_address_tole_label)) },
            placeholder = { Text(stringResource(Res.string.kyc_address_tole_placeholder)) },
            isError = currentValue.toleError != null,
            singleLine = true,
            supportingText = {
                currentValue.toleError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
    }
}

enum class AddressType {
    CURRENT,
    PERMANENT
}
