package com.vardansoft.authx.ui.kyc

import com.vardansoft.authx.data.DocumentType
import com.vardansoft.ui.generated.resources.Res
import com.vardansoft.ui.generated.resources.kyc_document_type_citizenship
import com.vardansoft.ui.generated.resources.kyc_document_type_driver_license
import com.vardansoft.ui.generated.resources.kyc_document_type_national_id
import com.vardansoft.ui.generated.resources.kyc_document_type_passport
import org.jetbrains.compose.resources.StringResource

fun DocumentType.toStringRes(): StringResource = when (this) {
    DocumentType.PASSPORT -> Res.string.kyc_document_type_passport
    DocumentType.DRIVER_LICENSE -> Res.string.kyc_document_type_driver_license
    DocumentType.NATIONAL_ID -> Res.string.kyc_document_type_national_id
    DocumentType.CITIZENSHIP -> Res.string.kyc_document_type_citizenship
}
