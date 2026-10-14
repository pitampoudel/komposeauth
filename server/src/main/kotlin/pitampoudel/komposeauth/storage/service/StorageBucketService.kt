package pitampoudel.komposeauth.storage.service

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.storage.dto.CreateBucketRequest
import pitampoudel.komposeauth.storage.dto.EditBucketRequest
import pitampoudel.komposeauth.storage.dto.EditBucketRequest.CredentialChange
import pitampoudel.komposeauth.storage.dto.ProbeResponse
import pitampoudel.komposeauth.storage.dto.ProbeResponse.Outcome
import pitampoudel.komposeauth.storage.dto.ProbeResponse.ProbeStep
import pitampoudel.komposeauth.storage.entity.StorageBucket
import pitampoudel.komposeauth.storage.entity.StorageBucket.Kind
import pitampoudel.komposeauth.storage.repository.StorageBucketRepository
import java.net.URI
import java.time.Instant
import java.util.UUID

/**
 * Every change to the list of buckets. Each one is a targeted update rather than a save of the whole
 * row, so an edit never puts back a primary flag or a copied-to bucket another admin changed meanwhile.
 *
 * Nothing here copies a file. Moving to another bucket is the admin's job, with the providers' own
 * tools; the list only records which bucket takes new files and which one a copied bucket's files
 * are now read from.
 */
@Service
class StorageBucketService(
    private val repository: StorageBucketRepository,
    private val mongoTemplate: MongoTemplate,
    private val buckets: StorageBuckets,
    private val crypto: CryptoService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun create(request: CreateBucketRequest): StorageBucket {
        val kind = request.kind
        val region = request.region.orNull()
        val endpoint = request.endpoint.orNull()?.let { baseUrl(it, "The endpoint") }
        val credentials = credentialsFor(kind, request.accessKeyId, request.secretAccessKey, request.serviceAccountJson)
        if (kind == Kind.S3 && region == null) {
            badRequest("An S3-compatible bucket needs a region: the AWS one it is in, or what the service says to use (auto on Cloudflare R2)")
        }
        if (region != null && !REGION.matches(region)) badRequest("That region is not one a bucket can be in")
        if (kind == Kind.GCS && (region != null || endpoint != null)) {
            badRequest("A Google Cloud Storage bucket has no region or endpoint to set here")
        }
        if (kind == Kind.S3 && !request.projectId.isNullOrBlank()) badRequest("Only a Google Cloud Storage bucket has a project")
        val candidate = StorageBucket(
            name = nameOf(request.name),
            kind = kind,
            bucketName = request.bucketName.trim().takeIf(BUCKET::matches)
                ?: badRequest("A bucket name is letters, digits, dots, dashes and underscores"),
            region = region,
            endpoint = endpoint,
            publicBaseUrl = request.publicBaseUrl.orNull()?.let { baseUrl(it, "The public address") },
            projectId = request.projectId.orNull(),
            accessKeyId = credentials.accessKeyId,
            secretAccessKey = credentials.secretAccessKey,
            serviceAccountJson = credentials.serviceAccountJson
        )
        buckets.all().firstOrNull { BucketAddresses.overlap(it.addresses, candidate.addresses()) }?.let {
            conflict("${it.row.name} already answers this bucket's addresses, so the two can't both be listed")
        }
        // The first bucket is where new files go; after that, which one does is a choice.
        val saved = repository.insert(encrypted(candidate.copy(primary = repository.count() == 0L)))
        buckets.forget()
        return saved
    }

    fun edit(id: ObjectId, request: EditBucketRequest): StorageBucket {
        val current = rowOf(id)
        if (current.kind == Kind.S3 && !request.projectId.isNullOrBlank()) badRequest("Only a Google Cloud Storage bucket has a project")
        val credentials = when (request.credentials) {
            CredentialChange.KEEP -> Credentials(current.accessKeyId, current.secretAccessKey, current.serviceAccountJson)
            CredentialChange.HOST -> Credentials(null, null, null)
            CredentialChange.REPLACE -> credentialsFor(
                current.kind, request.accessKeyId, request.secretAccessKey, request.serviceAccountJson
            ).also { if (it.isEmpty()) badRequest("Enter the new credentials, or choose the server's own identity") }
        }
        mongoTemplate.updateFirst(
            byId(id),
            Update()
                .set("name", nameOf(request.name))
                .set("projectId", request.projectId.orNull())
                .set("accessKeyId", credentials.accessKeyId)
                .set("secretAccessKey", credentials.secretAccessKey?.let(crypto::encrypt))
                .set("serviceAccountJson", credentials.serviceAccountJson?.let(crypto::encrypt))
                .set("updatedAt", Instant.now()),
            StorageBucket::class.java
        )
        buckets.forget()
        return rowOf(id)
    }

    /**
     * New files go to [id] and no other. The old primary is cleared first: the index allows one at
     * most, so for a moment there may be none, and an upload in that moment is refused rather than
     * sent to the wrong bucket.
     */
    fun makePrimary(id: ObjectId) {
        val bucket = rowOf(id)
        if (bucket.readFromBucketId != null) {
            conflict("${bucket.name}'s files open from ${nameOf(bucket.readFromBucketId)}, so new files can't go to it. Open its files from its own bucket first")
        }
        mongoTemplate.updateMulti(
            Query(Criteria.where("primary").`is`(true).and("_id").ne(id)),
            Update().set("primary", false).set("updatedAt", Instant.now()),
            StorageBucket::class.java
        )
        mongoTemplate.updateFirst(byId(id), Update().set("primary", true).set("updatedAt", Instant.now()), StorageBucket::class.java)
        buckets.forget()
    }

    /**
     * Records that an admin copied every file of [id] into [targetId] under the same names, so its
     * files are read from there and its own credential can be cleared. Nothing is checked or copied
     * here; [readOwnFiles] takes it back.
     */
    fun readFrom(id: ObjectId, targetId: ObjectId) {
        val bucket = rowOf(id)
        val target = buckets.byId(targetId)?.row ?: notFound()
        // A file written to it after the copy would not be in the copy.
        if (bucket.primary) conflict("New files go to ${bucket.name}; choose another bucket for them first")
        if (target.id == bucket.id) badRequest("Choose the bucket the files were copied to")
        // Its own files are read from somewhere else, so this bucket's would be too; which also rules out a loop.
        target.readFromBucketId?.let {
            conflict("${target.name}'s own files open from ${nameOf(it)}; choose the bucket the files are really in")
        }
        mongoTemplate.updateFirst(
            byId(id),
            Update().set("readFromBucketId", targetId).set("updatedAt", Instant.now()),
            StorageBucket::class.java
        )
        buckets.forget()
    }

    /** [id]'s files are read from its own bucket again: the undo of [readFrom], and the first step of moving back. */
    fun readOwnFiles(id: ObjectId) {
        rowOf(id)
        mongoTemplate.updateFirst(
            byId(id),
            Update().unset("readFromBucketId").set("updatedAt", Instant.now()),
            StorageBucket::class.java
        )
        buckets.forget()
    }

    /** Removes the entry, never the bucket or anything in it. */
    fun remove(id: ObjectId) {
        val bucket = rowOf(id)
        if (bucket.primary) conflict("New files go to ${bucket.name}; choose another bucket for them first")
        buckets.all().firstOrNull { it.row.readFromBucketId == id }?.let {
            conflict("${it.row.name}'s files are read from ${bucket.name}, so it has to stay listed")
        }
        // Its row is how the addresses already saved are still recognised as ours.
        bucket.readFromBucketId?.let {
            conflict("Saved addresses still name ${bucket.name}'s files, and this entry is how they open from ${nameOf(it)}")
        }
        repository.deleteById(id)
        buckets.forget()
    }

    /** Writes a file, reads it back and deletes it, reporting each step: the three things this server does in a bucket. */
    fun test(id: ObjectId): ProbeResponse {
        val entry = buckets.byId(id) ?: notFound()
        val name = "$PROBE_PREFIX${UUID.randomUUID()}"
        val content = "Written by the Storage page to test this bucket at ${Instant.now()}. Safe to delete.".toByteArray()
        val steps = mutableListOf<ProbeStep>()

        fun attempt(step: String, action: () -> String): Boolean =
            try {
                steps += ProbeStep(step, Outcome.OK, action())
                true
            } catch (ex: Exception) {
                log.warn("Storage test of {} failed at '{}'", entry.row.name, step, ex)
                steps += ProbeStep(step, Outcome.FAILED, ex.message?.take(400) ?: ex.javaClass.simpleName)
                false
            }

        val wrote = attempt("Write a file") {
            entry.client.upload(name, "text/plain", content)
            "Wrote $name."
        }
        if (wrote) {
            attempt("Read it back") {
                val back = entry.client.download(name) ?: error("Nothing was there to read")
                check(back.contentEquals(content)) { "It came back different from what was written" }
                "Read the same ${back.size} bytes back."
            }
            attempt("Delete it") {
                entry.client.delete(name)
                check(entry.client.stat(name) == null) { "It was still there afterwards" }
                "Deleted it."
            }
        } else {
            listOf("Read it back", "Delete it").forEach {
                steps += ProbeStep(it, Outcome.SKIPPED, "Not tried, since nothing was written.")
            }
        }
        return ProbeResponse(entry.row.name, steps.all { it.outcome == Outcome.OK }, steps)
    }

    private data class Credentials(val accessKeyId: String?, val secretAccessKey: String?, val serviceAccountJson: String?) {
        fun isEmpty() = accessKeyId == null && secretAccessKey == null && serviceAccountJson == null
    }

    private fun credentialsFor(kind: Kind, accessKeyId: String?, secretAccessKey: String?, serviceAccountJson: String?): Credentials {
        val keyId = accessKeyId.orNull()
        val secret = secretAccessKey.orNull()
        val json = serviceAccountJson.orNull()
        return when (kind) {
            Kind.S3 -> {
                if (json != null) badRequest("A service account key is for Google Cloud Storage; an S3-compatible bucket takes an access key")
                if ((keyId == null) != (secret == null)) badRequest("An access key needs both its ID and its secret")
                Credentials(keyId, secret, null)
            }

            Kind.GCS -> {
                if (keyId != null || secret != null) badRequest("An access key is for an S3-compatible bucket; Google Cloud Storage takes a service account key")
                json?.let(::serviceAccountEmail)
                Credentials(null, null, json)
            }
        }
    }

    private fun rowOf(id: ObjectId): StorageBucket {
        buckets.forget()
        return buckets.byId(id)?.row ?: notFound()
    }

    private fun nameOf(id: ObjectId): String = buckets.byId(id)?.row?.name ?: "another bucket"

    private fun encrypted(bucket: StorageBucket) = bucket.copy(
        secretAccessKey = bucket.secretAccessKey?.let(crypto::encrypt),
        serviceAccountJson = bucket.serviceAccountJson?.let(crypto::encrypt)
    )

    private fun byId(id: ObjectId) = Query(Criteria.where("_id").`is`(id))

    private fun nameOf(name: String) = name.trim().takeIf { it.isNotEmpty() && it.length <= 60 }
        ?: badRequest("Give the bucket a name of up to 60 characters")

    private fun String?.orNull() = this?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        const val PROBE_PREFIX = "komposeauth-storage-test/"
        private val BUCKET = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,221}")
        private val REGION = Regex("[A-Za-z0-9-]{1,40}")

        /**
         * An `http(s)://host[:port][/path]` address with no trailing slash. Plain http is allowed for
         * a MinIO on the same network.
         */
        fun baseUrl(value: String, what: String): String {
            val uri = runCatching { URI(value.trim().trimEnd('/')) }.getOrNull()
            val usable = uri != null && uri.scheme in setOf("http", "https") && !uri.host.isNullOrEmpty() &&
                uri.rawQuery == null && uri.rawFragment == null && uri.rawUserInfo == null
            if (!usable) badRequest("$what has to be an http or https address with nothing after the path, such as https://files.example.com")
            return uri.toString()
        }

        /** The service account a key signs in as, which is also how a key is told from anything else. */
        fun serviceAccountEmail(json: String): String =
            runCatching { Json.parseToJsonElement(json) as JsonObject }.getOrNull()
                ?.get("client_email")?.jsonPrimitive?.content
                ?: badRequest("That is not a service account key: paste the whole JSON file Google Cloud gave you")

        private fun badRequest(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)
        private fun conflict(message: String): Nothing = throw ResponseStatusException(HttpStatus.CONFLICT, message)
        private fun notFound(): Nothing = throw ResponseStatusException(HttpStatus.NOT_FOUND, "There is no such bucket")
    }
}
