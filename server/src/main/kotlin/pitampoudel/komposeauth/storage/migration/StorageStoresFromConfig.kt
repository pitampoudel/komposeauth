package pitampoudel.komposeauth.storage.migration

import com.mongodb.client.model.Filters
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.core.migration.MongoMigration
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.storage.entity.StorageStore
import java.time.Instant

/**
 * Lists the buckets the app config used to name as stores on the Storage page, once.
 *
 * The config held a GCS bucket and an S3 bucket in fixed fields, with `storageProvider` saying which
 * took new files and `gcsFilesReadFrom` saying the GCS files had been copied into S3. Each becomes a
 * row, with the same choice of where new files go and the same copied-to link, so nothing about
 * where a file is written or read from changes on the deployment that runs this.
 *
 * The old fields are read and never unset: a rollback to the release before this one still reads
 * them (see [AppConfig.RETIRED_STORAGE_FIELDS]).
 */
@Component
class StorageStoresFromConfig(private val crypto: CryptoService) : MongoMigration {
    private val log = LoggerFactory.getLogger(javaClass)

    override val id: String = "storage-stores-from-config"

    /** After the indexes, which keep a second primary out. */
    override val order: Int = 0

    override fun migrate(mongoTemplate: MongoTemplate) {
        val config = mongoTemplate.getCollection(mongoTemplate.getCollectionName(AppConfig::class.java))
            .find(Filters.eq("_id", AppConfig.SINGLETON_ID)).first() ?: return
        val existing = mongoTemplate.findAll(StorageStore::class.java)
        val carried = storesFrom(config, Instant.now())
        if (carried.isNotEmpty() && carried.none { it.primary }) {
            log.warn("The app config doesn't say which of its buckets takes new files, so none does until one is chosen on the Storage page")
        }
        carried
            .filter { row -> existing.none { it.kind == row.kind && it.bucket == row.bucket } }
            .map { row -> if (existing.any { it.primary }) row.copy(primary = false) else row }
            .forEach { row ->
                mongoTemplate.insert(row.copy(secretAccessKey = row.secretAccessKey?.let(crypto::encrypt)))
                log.info("Listed {} as a storage store{}", row, if (row.primary) ", taking new files" else "")
            }
    }

    companion object {
        /** How `storageProvider` and `gcsFilesReadFrom` named the two buckets. */
        private const val GCS = "gcs"
        private const val S3 = "s3"

        /**
         * The stores [config] (the stored `config` document) names, the way the release before this one
         * read it. Secrets are left as stored, which is already encrypted.
         */
        fun storesFrom(config: Document, now: Instant): List<StorageStore> {
            fun field(name: String) = config.getString(name)?.trim()?.takeIf { it.isNotEmpty() }

            val s3Bucket = field("s3BucketName")
            val s3Region = field("s3Region")
            if (s3Bucket != null && s3Region == null) {
                LoggerFactory.getLogger(StorageStoresFromConfig::class.java)
                    .error("s3BucketName {} has no s3Region, so it can't be listed; add it on the Storage page", s3Bucket)
            }
            val s3 = s3Bucket?.takeIf { s3Region != null }?.let { bucket ->
                StorageStore(
                    name = "Amazon S3",
                    kind = StorageStore.Kind.S3,
                    bucket = bucket,
                    region = s3Region,
                    accessKeyId = field("s3AccessKeyId"),
                    secretAccessKey = field("s3SecretAccessKey"),
                    createdAt = now,
                    updatedAt = now
                )
            }
            val gcsOnS3 = field("gcsFilesReadFrom")?.lowercase() == S3 && s3 != null
            val gcs = field("gcpBucketName")?.let { bucket ->
                StorageStore(
                    name = "Google Cloud Storage",
                    kind = StorageStore.Kind.GCS,
                    bucket = bucket,
                    projectId = field("gcpProjectId"),
                    readFromStoreId = if (gcsOnS3) s3?.id else null,
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
