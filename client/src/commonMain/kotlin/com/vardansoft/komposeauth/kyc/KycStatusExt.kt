package com.vardansoft.komposeauth.kyc

import androidx.compose.ui.graphics.Color
import com.vardansoft.komposeauth.data.KycResponse
import com.vardansoft.core.presentation.GREEN
import com.vardansoft.core.presentation.ORANGE
import com.vardansoft.core.presentation.RED
import com.vardansoft.client.generated.resources.Res
import com.vardansoft.client.generated.resources.kyc_status_approved
import com.vardansoft.client.generated.resources.kyc_status_draft
import com.vardansoft.client.generated.resources.kyc_status_pending
import com.vardansoft.client.generated.resources.kyc_status_rejected
import org.jetbrains.compose.resources.StringResource


fun KycResponse.Status.toColor(): Color = when (this) {
    KycResponse.Status.APPROVED -> GREEN
    KycResponse.Status.PENDING -> ORANGE
    KycResponse.Status.REJECTED -> RED
    KycResponse.Status.DRAFT -> Color.Gray
}


fun KycResponse.Status.toStringRes(): StringResource = when (this) {
    KycResponse.Status.APPROVED -> Res.string.kyc_status_approved
    KycResponse.Status.PENDING -> Res.string.kyc_status_pending
    KycResponse.Status.REJECTED -> Res.string.kyc_status_rejected
    KycResponse.Status.DRAFT -> Res.string.kyc_status_draft
}