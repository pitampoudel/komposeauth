package pitampoudel.komposeauth.kyc.dto


import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import pitampoudel.komposeauth.kyc.data.KycResponse
import pitampoudel.komposeauth.kyc.domain.DocumentType

/**
 * Third Factor sends `age` as a bare number at the top level but as a quoted string
 * inside faceDetectionLog. Accept either so one shape change cannot drop a webhook.
 */
object LenientIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeInt()
        return jsonDecoder.decodeJsonElement().jsonPrimitive.content.toIntOrNull()
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }
}

/**
 * Webhook payload delivered to the `callback` URL once a Third Factor SDK session ends.
 *
 * Mirrors https://third-factor.readthedocs.io/en/latest/endpoints.html#generate-kyc-url-sdk.
 * Every field is optional: the webhook is fire-and-forget, so a payload we cannot decode is a
 * verification we lose entirely. Absence is handled at the point of use instead.
 */
@Serializable
data class ThirdFactorModel(
    // IDENTITY — echoed back from the JWT we signed in ThirdFactorKycController.
    @SerialName("identifier")
    val identifier: String,
    @SerialName("jwt")
    val jwt: String,
    @SerialName("signature")
    val signature: String? = null,
    @SerialName("label")
    val label: String? = null,
    @SerialName("secondary_label")
    val secondaryLabel: String? = null,
    @SerialName("session")
    val session: String? = null,

    // VERDICT
    @SerialName("is_verified")
    val isVerified: Boolean? = null,
    @SerialName("documentDetectionSuccess")
    val documentDetectionSuccess: Boolean? = null,
    @SerialName("faceDetectionSuccess")
    val faceDetectionSuccess: Boolean? = null,
    @SerialName("gestureSuccess")
    val gestureSuccess: Boolean? = null,
    @SerialName("percentage_match")
    val percentageMatch: Double? = null,

    // INTEGRITY — a session that was bypassed or force-advanced did not pass on merit.
    @SerialName("bypassed")
    val bypassed: Int? = null,
    @SerialName("forced_next")
    val forcedNext: Boolean? = null,
    @SerialName("allow_force_next")
    val allowForceNext: Int? = null,
    @SerialName("in_progress")
    val inProgress: Int? = null,
    @SerialName("document_uplaod_retries") // [sic] — spelling is Third Factor's.
    val documentUploadRetries: Int? = null,
    @SerialName("face_detection_retries")
    val faceDetectionRetries: Int? = null,
    @SerialName("gesture_verification_retries")
    val gestureVerificationRetries: Int? = null,

    // EXTRACTED FROM THE DOCUMENT
    @SerialName("document_number")
    val documentNumber: String? = null,
    @SerialName("nationality")
    val nationality: String? = null,
    @SerialName("gender")
    val gender: String? = null,
    @Serializable(with = LenientIntSerializer::class)
    @SerialName("age")
    val age: Int? = null,

    // MEDIA
    @SerialName("userPhoto")
    val userPhoto: String? = null,
    @SerialName("gesture_photo")
    val gesturePhoto: String? = null,
    @SerialName("gesture_challenge")
    val gestureChallenge: List<String> = emptyList(),

    // LOGS
    @SerialName("documentDetectionLog")
    val documentDetectionLog: List<DocumentDetectionLog> = emptyList(),
    @SerialName("documentPhoto")
    val documentPhoto: List<DocumentPhoto> = emptyList(),
    @SerialName("gestureDetectionLog")
    val gestureDetectionLog: List<GestureDetectionLog> = emptyList(),
    @SerialName("faceDetectionLog")
    val faceDetectionLog: List<FaceDetectionLog> = emptyList(),

    // TIMESTAMPS
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("completed_at")
    val completedAt: String? = null,
    @SerialName("expires_at")
    val expiresAt: String? = null
) {
    @Serializable
    data class DocumentDetectionLog(
        @SerialName("created_at")
        val createdAt: String? = null,
        @SerialName("is_verified")
        val isVerified: Boolean = false,
        @SerialName("nationality")
        val nationality: String? = null,
        @SerialName("document_number")
        val documentNumber: String? = null,
        @SerialName("photo")
        val photo: String? = null,
        @SerialName("original_photo")
        val originalPhoto: String? = null,
        @SerialName("claimed_doc_type")
        val claimedDocType: String? = null,
        @SerialName("detected_doc_type")
        val detectedDocType: String? = null,
        @SerialName("percentage_match")
        val percentageMatch: Double? = null,
        @SerialName("reason")
        val reason: String? = null
    )

    @Serializable
    data class DocumentPhoto(
        @SerialName("created_at")
        val createdAt: String? = null,
        @SerialName("is_verified")
        val isVerified: Boolean = false,
        @SerialName("nationality")
        val nationality: String? = null,
        @SerialName("document_number")
        val documentNumber: String? = null,
        @SerialName("photo")
        val photo: String? = null,
        @SerialName("original_photo")
        val originalPhoto: String? = null,
        @SerialName("claimed_doc_type")
        val claimedDocType: String? = null,
        @SerialName("detected_doc_type")
        val detectedDocType: String? = null,
        @SerialName("percentage_match")
        val percentageMatch: Double? = null,
        @SerialName("reason")
        val reason: String? = null
    )

    @Serializable
    data class GestureDetectionLog(
        @SerialName("created_at")
        val createdAt: String? = null,
        @SerialName("is_verified")
        val isVerified: Boolean = false,
        @SerialName("challenged_gesture")
        val challengedGesture: String? = null,
        @SerialName("detected_gesture")
        val detectedGesture: String? = null,
        @SerialName("percentage_match")
        val percentageMatch: Double? = null,
        @SerialName("photo")
        val photo: String? = null,
        @SerialName("is_passive_live")
        val isPassiveLive: Boolean? = null,
        @SerialName("passive_score")
        val passiveScore: Double? = null,
        @SerialName("reason")
        val reason: String? = null
    )

    @Serializable
    data class FaceDetectionLog(
        @SerialName("created_at")
        val createdAt: String? = null,
        @SerialName("is_verified")
        val isVerified: Boolean = false,
        @SerialName("photo")
        val photo: String? = null,
        @Serializable(with = LenientIntSerializer::class)
        @SerialName("age")
        val age: Int? = null,
        @SerialName("gender")
        val gender: String? = null,
        @SerialName("force_next")
        val forceNext: Boolean? = null,
        @SerialName("reason")
        val reason: String? = null
    )
}

fun String?.toDocumentType(): DocumentType? = when {
    this == null -> null
    contains("citizenship", ignoreCase = true) -> DocumentType.CITIZENSHIP
    contains("passport", ignoreCase = true) -> DocumentType.PASSPORT
    contains("national", ignoreCase = true) -> DocumentType.NATIONAL_ID
    else -> null
}

/** What the model saw, falling back to what the user claimed only if nothing was detected. */
fun ThirdFactorModel.DocumentDetectionLog.docType(): DocumentType? =
    detectedDocType.toDocumentType() ?: claimedDocType.toDocumentType()

fun String.toGender(): KycResponse.Gender? = when {
    equals("male", ignoreCase = true) -> KycResponse.Gender.MALE
    equals("female", ignoreCase = true) -> KycResponse.Gender.FEMALE
    else -> null
}
