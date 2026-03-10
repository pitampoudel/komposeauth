package pitampoudel.komposeauth.kyc.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ThirdFactorModel(
    @SerialName("documentDetectionLog")
    val documentDetectionLog: List<DocumentDetectionLog>,
    @SerialName("document_number")
    val documentNumber: String,
    @SerialName("documentPhoto")
    val documentPhoto: List<DocumentPhoto>,
    @SerialName("expires_at")
    val expiresAt: String,
    @SerialName("gender")
    val gender: String,
    @SerialName("gesture_challenge")
    val gestureChallenge: List<String>,
    @SerialName("gesture_photo")
    val gesturePhoto: String,
    @SerialName("gestureSuccess")
    val gestureSuccess: Boolean,
    @SerialName("identifier")
    val identifier: String,
    @SerialName("in_progress")
    val inProgress: Int,
    @SerialName("is_verified")
    val isVerified: Boolean,
    @SerialName("jwt")
    val jwt: String,
    @SerialName("nationality")
    val nationality: String,
    @SerialName("percentage_match")
    val percentageMatch: Double,
    @SerialName("session")
    val session: String,
    @SerialName("signature")
    val signature: String,
    @SerialName("started_at")
    val startedAt: String,
    @SerialName("userPhoto")
    val userPhoto: String
) {
    @Serializable
    data class DocumentDetectionLog(
        @SerialName("claimed_doc_type")
        val claimedDocType: String,
        @SerialName("created_at")
        val createdAt: String,
        @SerialName("detected_doc_type")
        val detectedDocType: String,
        @SerialName("document_number")
        val documentNumber: String,
        @SerialName("is_verified")
        val isVerified: Boolean,
        @SerialName("nationality")
        val nationality: String,
        @SerialName("original_photo")
        val originalPhoto: String,
        @SerialName("percentage_match")
        val percentageMatch: Double? = null,
        @SerialName("photo")
        val photo: String?,
        @SerialName("reason")
        val reason: String
    )

    @Serializable
    data class DocumentPhoto(
        @SerialName("claimed_doc_type")
        val claimedDocType: String,
        @SerialName("created_at")
        val createdAt: String,
        @SerialName("detected_doc_type")
        val detectedDocType: String,
        @SerialName("document_number")
        val documentNumber: String,
        @SerialName("is_verified")
        val isVerified: Boolean,
        @SerialName("nationality")
        val nationality: String,
        @SerialName("photo")
        val photo: String,
        @SerialName("reason")
        val reason: String,
        @SerialName("original_photo")
        val originalPhoto: String? = null,
        @SerialName("percentage_match")
        val percentageMatch: Double? = null
    )


}