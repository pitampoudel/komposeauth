package pitampoudel.komposeauth.kyc

import androidx.compose.ui.graphics.Color
import io.github.pitampoudel.client.generated.resources.Res
import io.github.pitampoudel.client.generated.resources.kyc_status_approved
import io.github.pitampoudel.client.generated.resources.kyc_status_draft
import io.github.pitampoudel.client.generated.resources.kyc_status_pending
import io.github.pitampoudel.client.generated.resources.kyc_status_rejected
import org.jetbrains.compose.resources.StringResource
import pitampoudel.core.presentation.GREEN
import pitampoudel.core.presentation.ORANGE
import pitampoudel.core.presentation.RED
import pitampoudel.komposeauth.data.KycResponse


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