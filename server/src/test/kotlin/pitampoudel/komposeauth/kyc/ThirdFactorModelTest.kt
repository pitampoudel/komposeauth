package pitampoudel.komposeauth.kyc

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import pitampoudel.komposeauth.kyc.dto.ThirdFactorModel
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the contract with Third Factor's webhook. The payload below is copied verbatim from
 * https://third-factor.readthedocs.io/en/latest/endpoints.html#generate-kyc-url-sdk — if the docs
 * change, change this fixture, not the assertions.
 */
class ThirdFactorModelTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val documentedPayload = """
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
      "gesture_challenge": ["Thumb_Down"],
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
    """.trimIndent()

    @Test
    fun `decodes every field of the documented webhook payload`() {
        val model = json.decodeFromString<ThirdFactorModel>(documentedPayload)

        assertEquals("1715", model.identifier)
        assertEquals("<JWT_TOKEN>", model.jwt)
        assertEquals("<SIGNATURE>", model.signature)
        assertEquals("F7bgp2I", model.session)
        assertEquals("Jane User", model.label)
        assertEquals("jane", model.secondaryLabel)

        assertEquals(true, model.isVerified)
        assertEquals(true, model.documentDetectionSuccess)
        assertEquals(false, model.faceDetectionSuccess)
        assertEquals(true, model.gestureSuccess)
        assertEquals(64.7401, model.percentageMatch)

        assertEquals(1, model.bypassed)
        assertEquals(false, model.forcedNext)
        assertEquals(1, model.allowForceNext)
        assertEquals(0, model.inProgress)
        assertEquals(2, model.documentUploadRetries)
        assertEquals(1, model.faceDetectionRetries)
        assertEquals(1, model.gestureVerificationRetries)

        assertEquals("11111111", model.documentNumber)
        assertEquals("nepali", model.nationality)
        assertEquals("N/A", model.gender)
        assertEquals(-1, model.age)

        assertEquals("<BASE64_STRING_OF_USER>", model.userPhoto)
        assertEquals("<BASE64_STRING>", model.gesturePhoto)
        assertEquals(listOf("Thumb_Down"), model.gestureChallenge)

        assertEquals("2026-01-27T10:42:46.141586+00:00", model.startedAt)
        assertEquals("2026-01-27T10:43:27.882837+00:00", model.completedAt)
        assertEquals("2026-01-29T18:24:26.141586+00:00", model.expiresAt)

        val detection = model.documentDetectionLog.single()
        assertEquals("citizenship-back", detection.detectedDocType)
        assertEquals("citizenship-back", detection.claimedDocType)
        assertEquals("<BASE64_STRING_OF_ORIGINAL_FRAME>", detection.originalPhoto)
        assertTrue(detection.isVerified)

        // documentPhoto entries carry no original_photo in the documented payload.
        val photo = model.documentPhoto.single()
        assertEquals("<BASE64_STRING>", photo.photo)
        assertEquals(null, photo.originalPhoto)

        val gesture = model.gestureDetectionLog.single()
        assertEquals("Thumb_Down", gesture.challengedGesture)
        assertEquals(true, gesture.isPassiveLive)
        assertEquals(99.998, gesture.passiveScore)

        // age is a bare number at the top level but a quoted string in faceDetectionLog.
        val face = model.faceDetectionLog.single()
        assertEquals(-1, face.age)
        assertEquals(true, face.forceNext)
    }

    @Test
    fun `decodes a payload carrying only identifier and jwt`() {
        val model = json.decodeFromString<ThirdFactorModel>(
            """{"identifier":"1715","jwt":"<JWT_TOKEN>"}"""
        )

        assertEquals("1715", model.identifier)
        assertEquals(null, model.isVerified)
        assertTrue(model.documentDetectionLog.isEmpty())
        assertTrue(model.gestureChallenge.isEmpty())
    }
}
