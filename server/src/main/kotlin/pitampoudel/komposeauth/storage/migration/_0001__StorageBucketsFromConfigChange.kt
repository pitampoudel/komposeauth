package pitampoudel.komposeauth.storage.migration

import com.mongodb.client.model.Filters
import io.flamingock.api.RecoveryStrategy
import io.flamingock.api.annotations.Apply
import io.flamingock.api.annotations.Change
import io.flamingock.api.annotations.Recovery
import io.flamingock.api.annotations.Rollback
import io.flamingock.api.annotations.TargetSystem
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.core.config.FlamingockConfig
import pitampoudel.komposeauth.storage.entity.StorageBucket
import java.time.Instant

/**
 * Lists the buckets the app config used to name on the Storage page, once.
 *
 * The config held a GCS bucket and an S3 bucket in fixed fields, with `storageProvider` saying which
 * took new files and `gcsFilesReadFrom` saying the GCS files had been copied into S3. Each becomes a
 * row, with the same choice of where new files go and the same copied-to link, so nothing about
 * where a file is written or read from changes on the deployment that runs this.
 *
 * The old fields are read and never unset: a rollback to the release before this one still reads
 * them (see [AppConfig.RETIRED_STORAGE_FIELDS]).
 */
@Change(id = "storage-buckets-from-config", author = "komposeauth", transactional = false)
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Recovery(strategy = RecoveryStrategy.ALWAYS_RETRY)
class _0001__StorageBucketsFromConfigChange {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Safe to run again: a bucket already listed is not listed twice. */
    @Apply
    fun apply(mongoTemplate: MongoTemplate) {
        val config = mongoTemplate.getCollection(mongoTemplate.getCollectionName(AppConfig::class.java))
            .find(Filters.eq("_id", AppConfig.SINGLETON_ID)).first() ?: return
        val existing = mongoTemplate.findAll(StorageBucket::class.java)
        val carried = bucketsFrom(config, Instant.now())
        if (carried.isNotEmpty() && carried.none { it.primary }) {
            log.warn("The app config doesn't say which of its buckets takes new files, so none does until one is chosen on the Storage page")
        }
        carried
            .filter { row -> existing.none { it.kind == row.kind && it.bucketName == row.bucketName } }
            .map { row -> if (existing.any { it.primary }) row.copy(primary = false) else row }
            .forEach { row ->
                mongoTemplate.insert(row)
                log.info("Listed {} as a storage bucket{}", row, if (row.primary) ", taking new files" else "")
            }
    }

    /** Nothing to undo: the config is never changed, and a run that failed lists the rest next time. */
    @Rollback
    fun rollback() = Unit

    companion object {
        /** How `storageProvider` and `gcsFilesReadFrom` named the two buckets. */
        private const val GCS = "gcs"
        private const val S3 = "s3"

        /**
         * The buckets [config] (the stored `config` document) names, the way the release before this one
         * read it. Secrets are left as stored, which is already encrypted.
         */
        fun bucketsFrom(config: Document, now: Instant): List<StorageBucket> {
            fun field(name: String) = config.getString(name)?.trim()?.takeIf { it.isNotEmpty() }

            val s3Bucket = field("s3BucketName")
            val s3Region = field("s3Region")
            if (s3Bucket != null && s3Region == null) {
                LoggerFactory.getLogger(_0001__StorageBucketsFromConfigChange::class.java)
                    .error("s3BucketName {} has no s3Region, so it can't be listed; add it on the Storage page", s3Bucket)
            }
            val s3 = s3Bucket?.takeIf { s3Region != null }?.let { bucketName ->
                StorageBucket(
                    name = "Amazon S3",
                    kind = StorageBucket.Kind.S3,
                    bucketName = bucketName,
                    region = s3Region,
                    accessKeyId = field("s3AccessKeyId"),
                    secretAccessKey = field("s3SecretAccessKey"),
                    createdAt = now,
                    updatedAt = now
                )
            }
            val gcsOnS3 = field("gcsFilesReadFrom")?.lowercase() == S3 && s3 != null
            val gcs = field("gcpBucketName")?.let { bucketName ->
                StorageBucket(
                    name = "Google Cloud Storage",
                    kind = StorageBucket.Kind.GCS,
                    bucketName = bucketName,
                    projectId = field("gcpProjectId"),
                    readFromBucketId = if (gcsOnS3) s3?.id else null,
                    // Listed first, being the older of the two wherever both are set.
                    createdAt = now.minusMillis(1),
                    updatedAt = now
                )
            }

            val takesNewFiles = listOfNotNull(
                (GCS to gcs).takeIf { gcs != null && !gcsOnS3 },
                (S3 to s3).takeIf { s3 != null }
            ).toMap()
            val named = field("storageProvider")?.lowercase()
            val primary = if (named != null) takesNewFiles[named] else takesNewFiles.values.singleOrNull()
            return listOfNotNull(gcs, s3).map { it.copy(primary = it.id == primary?.id) }
        }
    }
}
