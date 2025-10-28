package pitampoudel.komposeauth.kyc

import androidx.compose.ui.graphics.Color
import io.github.pitampoudel.client.generated.resources.Res
import io.github.pitampoudel.client.generated.resources.kyc_document_type_citizenship
import io.github.pitampoudel.client.generated.resources.kyc_document_type_national_id
import io.github.pitampoudel.client.generated.resources.kyc_document_type_passport
import io.github.pitampoudel.client.generated.resources.kyc_gender_female
import io.github.pitampoudel.client.generated.resources.kyc_gender_male
import io.github.pitampoudel.client.generated.resources.kyc_gender_other
import io.github.pitampoudel.client.generated.resources.kyc_marital_status_married
import io.github.pitampoudel.client.generated.resources.kyc_marital_status_unmarried
import io.github.pitampoudel.client.generated.resources.kyc_status_approved
import io.github.pitampoudel.client.generated.resources.kyc_status_draft
import io.github.pitampoudel.client.generated.resources.kyc_status_pending
import io.github.pitampoudel.client.generated.resources.kyc_status_rejected
import org.jetbrains.compose.resources.StringResource
import pitampoudel.core.presentation.GREEN
import pitampoudel.core.presentation.ORANGE
import pitampoudel.core.presentation.RED
import pitampoudel.komposeauth.data.KycResponse
import pitampoudel.komposeauth.domain.DocumentType

fun DocumentType.toStringRes(): StringResource = when (this) {
    DocumentType.PASSPORT -> Res.string.kyc_document_type_passport
    DocumentType.NATIONAL_ID -> Res.string.kyc_document_type_national_id
    DocumentType.CITIZENSHIP -> Res.string.kyc_document_type_citizenship
}

fun KycResponse.Gender.toStringRes(): StringResource = when (this) {
    KycResponse.Gender.MALE -> Res.string.kyc_gender_male
    KycResponse.Gender.FEMALE -> Res.string.kyc_gender_female
    KycResponse.Gender.OTHER -> Res.string.kyc_gender_other
}

fun KycResponse.MaritalStatus.toStringRes(): StringResource = when (this) {
    KycResponse.MaritalStatus.MARRIED -> Res.string.kyc_marital_status_married
    KycResponse.MaritalStatus.UNMARRIED -> Res.string.kyc_marital_status_unmarried
}


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