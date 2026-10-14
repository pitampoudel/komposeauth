package pitampoudel.komposeauth.storage.dto

import kotlinx.serialization.Serializable
import pitampoudel.komposeauth.storage.entity.StorageStore

@Serializable
data class CreateStoreRequest(
    val name: String,
    val kind: StorageStore.Kind,
    val bucket: String,
    val region: String? = null,
    val endpoint: String? = null,
    val publicBaseUrl: String? = null,
    val projectId: String? = null,
    /** All of these empty: the host's own identity. */
    val accessKeyId: String? = null,
    val secretAccessKey: String? = null,
    val serviceAccountJson: String? = null
)

/** Only what does not change the store's addresses can be edited. */
@Serializable
data class EditStoreRequest(
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

@Serializable
data class StartMoveRequest(val fromStoreId: String, val toStoreId: String)

data class StoreResponse(
    val id: String,
    val name: String,
    val kind: String,
    val kindLabel: String,
    val bucket: String,
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
    val readFromStoreId: String?,
    val readFromName: String?,
    /** The stores whose files are read from this one. */
    val readBy: List<String>,
    /** What the store is for now, in plain words. */
    val role: String
)

data class MoveResponse(
    val id: String,
    val fromStoreId: String,
    val toStoreId: String,
    val fromName: String,
    val toName: String,
    val state: String,
    val active: Boolean,
    /** What is happening, or what happened, in a line. */
    val summary: String,
    val startedBy: String?,
    val readNotBefore: String,
    val filesTotal: Long,
    val bytesTotal: Long,
    val filesCopied: Long,
    val bytesCopied: Long,
    val filesAlreadyThere: Long,
    val filesGone: Long,
    val filesFailed: Long,
    val failedNames: List<String>,
    val recordsTotal: Long,
    val recordsRead: Long,
    val linksUpdated: Long,
    val linksLeft: Long,
    val error: String?,
    val startedAt: String,
    val updatedAt: String,
    val finishedAt: String?
)

data class StorageStateResponse(
    val canManage: Boolean,
    val stores: List<StoreResponse>,
    /** The move under way, or else the last one, so its outcome stays readable. */
    val move: MoveResponse?,
    /** Something an admin has to put right, such as no store taking new files. */
    val problem: String?
)

data class ProbeResponse(val storeName: String, val ok: Boolean, val steps: List<ProbeStep>) {
    data class ProbeStep(val step: String, val outcome: Outcome, val detail: String)

    enum class Outcome { OK, FAILED, SKIPPED }
}
