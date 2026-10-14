package pitampoudel.komposeauth.storage.dto

import pitampoudel.komposeauth.storage.entity.StorageMove
import pitampoudel.komposeauth.storage.entity.StorageMove.State
import pitampoudel.komposeauth.storage.entity.StorageStore
import pitampoudel.komposeauth.storage.service.StorageMoveService
import pitampoudel.komposeauth.storage.service.StorageStoreService

/** [store] as the Storage page lists it, among [all] of them; [store] is decrypted. */
fun storeResponse(store: StorageStore, all: List<StorageStore>): StoreResponse {
    val readFrom = store.readFromStoreId?.let { id -> all.firstOrNull { it.id == id } }
    return StoreResponse(
        id = store.id.toHexString(),
        name = store.name,
        kind = store.kind.name,
        kindLabel = store.kind.label,
        bucket = store.bucket,
        region = store.region,
        endpoint = store.endpoint,
        publicBaseUrl = store.publicBaseUrl,
        projectId = store.projectId,
        location = locationOf(store),
        credentials = credentialsOf(store),
        usesHostIdentity = store.usesHostIdentity(),
        primary = store.primary,
        readFromStoreId = readFrom?.id?.toHexString(),
        readFromName = readFrom?.name,
        readBy = all.filter { it.readFromStoreId == store.id }.map { it.name },
        role = when {
            store.primary -> "New files go here"
            readFrom != null -> "Copied to ${readFrom.name} — its files open from there"
            else -> "Holds older files"
        }
    )
}

private fun locationOf(store: StorageStore): String {
    val where = when (store.kind) {
        StorageStore.Kind.GCS -> "Google Cloud Storage bucket ${store.bucket}" +
            (store.projectId?.let { " in project $it" } ?: "")

        StorageStore.Kind.S3 -> store.endpoint
            ?.let { "Bucket ${store.bucket} at $it, region ${store.region}" }
            ?: "Amazon S3 bucket ${store.bucket} in ${store.region}"
    }
    return where + (store.publicBaseUrl?.let { "; files are handed out under $it" } ?: "")
}

private fun credentialsOf(store: StorageStore): String = when {
    store.accessKeyId != null -> "Access key ${store.accessKeyId}"
    store.serviceAccountJson != null ->
        "Service account " + runCatching { StorageStoreService.serviceAccountEmail(store.serviceAccountJson) }.getOrDefault("key")
    else -> "The server's own identity: the role or service account it runs as"
}

fun StorageMove.toResponse() = MoveResponse(
    id = id.toHexString(),
    fromStoreId = fromStoreId.toHexString(),
    toStoreId = toStoreId.toHexString(),
    fromName = fromName,
    toName = toName,
    state = state.name,
    active = active,
    summary = summaryOf(this),
    startedBy = startedBy,
    readNotBefore = readNotBefore.toString(),
    filesTotal = filesTotal,
    bytesTotal = bytesTotal,
    filesCopied = filesCopied,
    bytesCopied = bytesCopied,
    filesAlreadyThere = filesAlreadyThere,
    filesGone = filesGone,
    filesFailed = filesFailed,
    failedNames = failedNames,
    recordsTotal = recordsTotal,
    recordsRead = recordsRead,
    linksUpdated = linksUpdated,
    linksLeft = linksLeft,
    error = error,
    startedAt = startedAt.toString(),
    updatedAt = updatedAt.toString(),
    finishedAt = finishedAt?.toString()
)

/** What the move is doing, or what it did, with every figure against what it is out of. */
fun summaryOf(move: StorageMove): String {
    val files = "${StorageMoveService.done(move)} of ${move.filesTotal} files"
    val records = "${move.recordsRead} of ${move.recordsTotal} records"
    return when (move.state) {
        State.SWITCHING ->
            "New files go to ${move.toName} now. Copying begins once every server has switched, in under a minute."

        State.COUNTING -> "Counting ${move.fromName}'s files: ${move.filesTotal} so far."

        State.COPYING -> "Copying: $files dealt with — ${move.filesCopied} copied, ${move.filesAlreadyThere} already " +
            "there, ${move.filesGone} deleted meanwhile, ${move.filesFailed} failed."

        State.REWRITING -> "All ${move.filesTotal} files are in ${move.toName} and ${move.fromName}'s addresses open " +
            "them from there. Updating saved addresses: $records read, ${move.linksUpdated} updated."

        State.DONE -> "Done. ${count(move.filesTotal - move.filesGone, "file is", "files are")} in ${move.toName}, and " +
            "${count(move.linksUpdated, "saved address now points", "saved addresses now point")} there, with $records " +
            "read. " + when (move.linksLeft) {
                0L -> "Nothing left on record points at ${move.fromName}, so its entry can be removed."
                1L -> "1 mention of its files was left as it was (inside longer text, or changed while this ran) and " +
                    "still opens from ${move.toName}; keep ${move.fromName} listed, or it stops opening."
                else -> "${move.linksLeft} mentions of its files were left as they were (inside longer text, or changed " +
                    "while this ran) and still open from ${move.toName}; keep ${move.fromName} listed, or they stop opening."
            }

        State.FAILED -> "Stopped: ${move.error ?: "something went wrong"}"

        State.CANCELLED -> "Cancelled after $files and $records. New files still go to ${move.toName}, and every " +
            "saved address still opens."
    }
}

private fun count(n: Long, one: String, many: String) = "$n ${if (n == 1L) one else many}"
