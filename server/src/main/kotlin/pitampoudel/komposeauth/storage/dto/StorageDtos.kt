package pitampoudel.komposeauth.storage.dto

import kotlinx.serialization.Serializable
import pitampoudel.komposeauth.storage.entity.StorageBucket

@Serializable
data class CreateBucketRequest(
    val name: String,
    val kind: StorageBucket.Kind,
    val bucketName: String,
    val region: String? = null,
    val endpoint: String? = null,
    val publicBaseUrl: String? = null,
    val projectId: String? = null,
    /** All of these empty: the host's own identity. */
    val accessKeyId: String? = null,
    val secretAccessKey: String? = null,
    val serviceAccountJson: String? = null
)

/** Only what does not change the bucket's addresses can be edited. */
@Serializable
data class EditBucketRequest(
    val name: String,
    val projectId: String? = null,
    val credentials: CredentialChange = CredentialChange.KEEP,
    val accessKeyId: String? = null,
    val secretAccessKey: String? = null,
    val serviceAccountJson: String? = null
) {
    enum class CredentialChange {
        KEEP,
        /** Sign in as the host's own identity, holding no credential. */
        HOST,
        /** With the credentials given here. */
        REPLACE
    }
}

/** The bucket an admin copied this one's files to. */
@Serializable
data class ReadFromRequest(val bucketId: String)

data class BucketResponse(
    val id: String,
    val name: String,
    val kind: String,
    val kindLabel: String,
    val bucketName: String,
    val region: String?,
    val endpoint: String?,
    val publicBaseUrl: String?,
    val projectId: String?,
    /** Where the bucket is, in a line. */
    val location: String,
    /** How the server signs in to it, in a line. */
    val credentials: String,
    val usesHostIdentity: Boolean,
    val primary: Boolean,
    val readFromBucketId: String?,
    val readFromName: String?,
    /** The buckets whose files are read from this one. */
    val readBy: List<String>,
    /** What the bucket is for now, in plain words. */
    val role: String
)

data class StorageStateResponse(
    val canManage: Boolean,
    val buckets: List<BucketResponse>,
    /** Something an admin has to put right, such as no bucket taking new files. */
    val problem: String?
)

data class ProbeResponse(val bucketName: String, val ok: Boolean, val steps: List<ProbeStep>) {
    data class ProbeStep(val step: String, val outcome: Outcome, val detail: String)

    enum class Outcome { OK, FAILED, SKIPPED }
}
