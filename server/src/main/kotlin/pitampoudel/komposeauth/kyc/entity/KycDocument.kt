package pitampoudel.komposeauth.kyc.entity

import kotlin.reflect.KProperty1

/** The files a KYC submission carries, each under the name it is stored and read back by. */
enum class KycDocument(val label: String, val storedUrl: KProperty1<KycVerification, String?>) {
    FRONT("front", KycVerification::documentFrontUrl),
    BACK("back", KycVerification::documentBackUrl),
    SELFIE("selfie", KycVerification::selfieUrl);

    companion object {
        fun ofLabel(label: String): KycDocument? = entries.firstOrNull { it.label == label }
    }
}
