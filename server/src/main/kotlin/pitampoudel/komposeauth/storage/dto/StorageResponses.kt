package pitampoudel.komposeauth.storage.dto

import pitampoudel.komposeauth.storage.entity.StorageBucket
import pitampoudel.komposeauth.storage.service.StorageBucketService

/** [bucket] as the Storage page lists it, among [all] of them; [bucket] is decrypted. */
fun bucketResponse(bucket: StorageBucket, all: List<StorageBucket>): BucketResponse {
    val readFrom = bucket.readFromBucketId?.let { id -> all.firstOrNull { it.id == id } }
    return BucketResponse(
        id = bucket.id.toHexString(),
        name = bucket.name,
        kind = bucket.kind.name,
        kindLabel = bucket.kind.label,
        bucketName = bucket.bucketName,
        region = bucket.region,
        endpoint = bucket.endpoint,
        publicBaseUrl = bucket.publicBaseUrl,
        projectId = bucket.projectId,
        location = locationOf(bucket),
        credentials = credentialsOf(bucket),
        usesHostIdentity = bucket.usesHostIdentity(),
        primary = bucket.primary,
        readFromBucketId = readFrom?.id?.toHexString(),
        readFromName = readFrom?.name,
        readBy = all.filter { it.readFromBucketId == bucket.id }.map { it.name },
        role = when {
            bucket.primary -> "New files go here"
            readFrom != null -> "Copied to ${readFrom.name} — its files open from there"
            else -> "Holds older files"
        }
    )
}

private fun locationOf(bucket: StorageBucket): String {
    val where = when (bucket.kind) {
        StorageBucket.Kind.GCS -> "Google Cloud Storage bucket ${bucket.bucketName}" +
            (bucket.projectId?.let { " in project $it" } ?: "")

        StorageBucket.Kind.S3 -> bucket.endpoint
            ?.let { "Bucket ${bucket.bucketName} at $it, region ${bucket.region}" }
            ?: "Amazon S3 bucket ${bucket.bucketName} in ${bucket.region}"
    }
    return where + (bucket.publicBaseUrl?.let { "; files are handed out under $it" } ?: "")
}

private fun credentialsOf(bucket: StorageBucket): String = when {
    bucket.accessKeyId != null -> "Access key ${bucket.accessKeyId}"
    bucket.serviceAccountJson != null ->
        "Service account " + runCatching { StorageBucketService.serviceAccountEmail(bucket.serviceAccountJson) }.getOrDefault("key")
    else -> "The server's own identity: the role or service account it runs as"
}
