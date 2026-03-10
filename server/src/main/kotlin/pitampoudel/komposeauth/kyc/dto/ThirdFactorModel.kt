package pitampoudel.komposeauth.kyc.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
Sample payload
{
"documentDetectionLog": [
{
"created_at": "2026-01-27 10:43:26.644142+00:00",
"is_verified": true,
"nationality": "nepali",
"document_number": "11111111",
"photo": "<BASE64_STRING_OF_SCANNED_DOC>",
"original_photo": "<BASE64_STRING_OF_ORIGINAL_FRAME>",
"claimed_doc_type": "citizenship-back",
"detected_doc_type": "citizenship-back",
"reason": "Valid Type"
},
{
"created_at": "2026-01-27 10:43:27.201931+00:00",
"is_verified": true,
"nationality": "nepali",
"document_number": "11111111",
"photo": "<BASE64_STRING>",
"original_photo": "<BASE64_STRING>",
"percentage_match": 64.7401,
"claimed_doc_type": "citizenship-front",
"detected_doc_type": "citizenship-front",
"reason": "Valid"
}
],
"documentPhoto": [
{
"created_at": "2026-01-27 10:43:26.644142+00:00",
"is_verified": true,
"nationality": "nepali",
"document_number": "11111111",
"photo": "<BASE64_STRING>",
"claimed_doc_type": "citizenship-back",
"detected_doc_type": "citizenship-back",
"reason": "Valid Type"
}
],
"gestureDetectionLog": [
{
"created_at": "2026-01-27 10:43:10.255518+00:00",
"is_verified": true,
"challenged_gesture": "Thumb_Down",
"detected_gesture": "Thumb_Down",
"percentage_match": 80.8998,
"photo": "<BASE64_STRING_OF_GESTURE>",
"reason": "Valid",
"is_passive_live": true,
"passive_score": 99.998
}
],
"faceDetectionLog": [
{
"created_at": "2026-01-27 10:42:58.538744+00:00",
"is_verified": false,
"photo": "<BASE64_STRING_OF_FACE>",
"age": "-1",
"gender": "N/A",
"reason": "Face Occluded or Blurry.",
"force_next": true
}
],
"nationality": "nepali",
"completed_at": "2026-01-27T10:43:27.882837+00:00",
"bypassed": 1,
"document_uplaod_retries": 2,
"session": "F7bgp2I",
"faceDetectionSuccess": false,
"document_number": "11111111",
"gestureSuccess": true,
"gender": "N/A",
"documentDetectionSuccess": true,
"in_progress": 0,
"age": -1,
"expires_at": "2026-01-29T18:24:26.141586+00:00",
"userPhoto": "<BASE64_STRING_OF_USER>",
"gesture_photo": "<BASE64_STRING>",
"gesture_challenge": [
"Thumb_Down"
],
"percentage_match": 64.7401,
"face_detection_retries": 1,
"allow_force_next": 1,
"started_at": "2026-01-27T10:42:46.141586+00:00",
"gesture_verification_retries": 1,
"label": "Jane User",
"secondary_label": "jane",
"identifier": "1715",
"jwt": "<JWT_TOKEN>",
"is_verified": true,
"forced_next": false,
"signature": "<SIGNATURE>"
}
 */
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
    @SerialName("faceDetectionLog")
    val faceDetectionLog: List<FaceDetectionLog>,
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
    @SerialName("gestureDetectionLog")
    val gestureDetectionLog: List<GestureDetectionLog>,
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
        val photo: String,
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

    @Serializable
    data class FaceDetectionLog(
        @SerialName("age")
        val age: String,
        @SerialName("created_at")
        val createdAt: String,
        @SerialName("force_next")
        val forceNext: Boolean,
        @SerialName("gender")
        val gender: String,
        @SerialName("is_verified")
        val isVerified: Boolean,
        @SerialName("photo")
        val photo: String,
        @SerialName("reason")
        val reason: String
    )

    @Serializable
    data class GestureDetectionLog(
        @SerialName("challenged_gesture")
        val challengedGesture: String,
        @SerialName("created_at")
        val createdAt: String,
        @SerialName("detected_gesture")
        val detectedGesture: String,
        @SerialName("is_passive_live")
        val isPassiveLive: Boolean,
        @SerialName("is_verified")
        val isVerified: Boolean,
        @SerialName("passive_score")
        val passiveScore: Double,
        @SerialName("percentage_match")
        val percentageMatch: Double,
        @SerialName("photo")
        val photo: String,
        @SerialName("reason")
        val reason: String
    )
}