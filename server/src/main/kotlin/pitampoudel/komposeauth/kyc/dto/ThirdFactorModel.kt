package pitampoudel.komposeauth.kyc.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// Note: The photo, original_photo, userPhoto, and gesture_photo fields contain the full Base64 string of the respective images.
@Serializable
data class ThirdFactorModel(
    @SerialName("age")
    val age: Int,
    @SerialName("allow_force_next")
    val allowForceNext: Int,
    @SerialName("bypassed")
    val bypassed: Int,
    @SerialName("completed_at")
    val completedAt: String,
    @SerialName("documentDetectionLog")
    val documentDetectionLog: List<DocumentDetectionLog>,
    @SerialName("documentDetectionSuccess")
    val documentDetectionSuccess: Boolean,
    @SerialName("document_number")
    val documentNumber: String,
    @SerialName("documentPhoto")
    val documentPhoto: List<DocumentPhoto>,
    @SerialName("document_uplaod_retries")
    val documentUplaodRetries: Int,
    @SerialName("expires_at")
    val expiresAt: String,
    @SerialName("face_detection_retries")
    val faceDetectionRetries: Int,
    @SerialName("faceDetectionSuccess")
    val faceDetectionSuccess: Boolean,
    @SerialName("forced_next")
    val forcedNext: Boolean,
    @SerialName("gender")
    val gender: String,
    @SerialName("gesture_challenge")
    val gestureChallenge: List<String>,
    @SerialName("gesture_photo")
    val gesturePhoto: String,
    @SerialName("gestureSuccess")
    val gestureSuccess: Boolean,
    @SerialName("gesture_verification_retries")
    val gestureVerificationRetries: Int,
    @SerialName("identifier")
    val identifier: String,
    @SerialName("in_progress")
    val inProgress: Int,
    @SerialName("is_verified")
    val isVerified: Boolean,
    @SerialName("jwt")
    val jwt: String,
    @SerialName("label")
    val label: String,
    @SerialName("nationality")
    val nationality: String,
    @SerialName("percentage_match")
    val percentageMatch: Double,
    @SerialName("secondary_label")
    val secondaryLabel: String,
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