package com.vardansoft.komposeauth.ui.kyc

import com.vardansoft.komposeauth.domain.DocumentType
import com.vardansoft.komposeauth.data.KycResponse
import com.vardansoft.ui.generated.resources.Res
import com.vardansoft.ui.generated.resources.kyc_document_type_citizenship
import com.vardansoft.ui.generated.resources.kyc_document_type_national_id
import com.vardansoft.ui.generated.resources.kyc_document_type_passport
import com.vardansoft.ui.generated.resources.kyc_gender_female
import com.vardansoft.ui.generated.resources.kyc_gender_male
import com.vardansoft.ui.generated.resources.kyc_gender_other
import com.vardansoft.ui.generated.resources.kyc_marital_status_married
import com.vardansoft.ui.generated.resources.kyc_marital_status_unmarried
import org.jetbrains.compose.resources.StringResource

fun DocumentType.toStringRes(): StringResource = when (this) {
    DocumentType.PASSPORT -> Res.string.kyc_document_type_passport
    DocumentType.NATIONAL_ID -> Res.string.kyc_document_type_national_id
    DocumentType.CITIZENSHIP -> Res.string.kyc_document_type_citizenship
}

fun KycResponse.Gender.toStringRes(): StringResource = when(this){
    KycResponse.Gender.MALE -> Res.string.kyc_gender_male
    KycResponse.Gender.FEMALE -> Res.string.kyc_gender_female
    KycResponse.Gender.OTHER -> Res.string.kyc_gender_other
}

fun KycResponse.MaritalStatus.toStringRes(): StringResource = when(this){
    KycResponse.MaritalStatus.MARRIED -> Res.string.kyc_marital_status_married
    KycResponse.MaritalStatus.UNMARRIED -> Res.string.kyc_marital_status_unmarried
}
