package pitampoudel.komposeauth.storage

import org.bson.Document
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.app_config.entity.AppConfig
import pitampoudel.komposeauth.app_config.service.AppConfigProvider
import pitampoudel.komposeauth.core.service.StorageService
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.storage.entity.StorageStore
import pitampoudel.komposeauth.storage.migration.StorageStoresFromConfig
import pitampoudel.komposeauth.storage.repository.StorageStoreRepository
import pitampoudel.komposeauth.storage.service.StorageStores
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The buckets the config named are carried onto the Storage page once, and the config keeps them,
 * saves included, so the release before can still be rolled back to.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class StorageStoresFromConfigIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var appConfigProvider: AppConfigProvider

    @Autowired
    private lateinit var crypto: CryptoService

    @Autowired
    private lateinit var storeRepository: StorageStoreRepository

    @Autowired
    private lateinit var stores: StorageStores

    @Autowired
    private lateinit var storageService: StorageService

    private val singleton = Query(Criteria.where("_id").`is`(AppConfig.SINGLETON_ID))
    private var configExisted = false

    private fun storedConfig(): Document =
        mongoTemplate.getCollection("config").find(Document("_id", AppConfig.SINGLETON_ID)).first()!!

    @BeforeEach
    fun setUp() {
        configExisted = mongoTemplate.exists(singleton, "config")
        storeRepository.deleteAll()
        stores.forget()
    }

    @AfterEach
    fun tearDown() {
        storeRepository.deleteAll()
        stores.forget()
        if (configExisted) {
            mongoTemplate.updateFirst(
                singleton,
                Update().apply { AppConfig.RETIRED_STORAGE_FIELDS.forEach { unset(it) } },
                "config"
            )
        } else {
            mongoTemplate.remove(singleton, "config")
        }
        appConfigProvider.clearCache()
    }

    @Test
    fun `the config's buckets are listed once and the config still has them afterwards, saves included`() {
        val secret = crypto.encrypt("s3-secret")
        mongoTemplate.upsert(
            singleton,
            Update()
                .set("storageProvider", "s3")
                .set("gcpProjectId", "legacy-project")
                .set("gcpBucketName", "legacy-gcs-files")
                .set("s3BucketName", "legacy-s3-files")
                .set("s3Region", "ap-south-1")
                .set("s3AccessKeyId", "AKIALEGACY")
                .set("s3SecretAccessKey", secret),
            "config"
        )

        val migration = StorageStoresFromConfig(crypto)
        migration.migrate(mongoTemplate)
        migration.migrate(mongoTemplate)

        val rows = storeRepository.findAll().sortedBy(StorageStore::createdAt)
        assertEquals(listOf("legacy-gcs-files", "legacy-s3-files"), rows.map { it.bucket }, "listed once, not twice")
        assertEquals(listOf(false, true), rows.map { it.primary })
        assertEquals("s3-secret", crypto.decrypt(rows[1].secretAccessKey!!))
        assertTrue(crypto.isEncrypted(rows[1].secretAccessKey), "kept encrypted at rest")

        stores.forget()
        assertEquals(
            "users/u1/photo",
            storageService.blobNameOf("https://legacy-s3-files.s3.ap-south-1.amazonaws.com/users/u1/photo")
        )
        assertEquals("legacy-s3-files", stores.primary()?.row?.bucket)

        val afterMigration = storedConfig()
        assertEquals("legacy-gcs-files", afterMigration.getString("gcpBucketName"))
        assertEquals("s3", afterMigration.getString("storageProvider"))
        assertEquals(secret, afterMigration.getString("s3SecretAccessKey"))

        // A save through the configuration page replaces the whole document.
        appConfigProvider.clearCache()
        appConfigProvider.save(appConfigProvider.get())

        val afterSave = storedConfig()
        AppConfig.RETIRED_STORAGE_FIELDS.forEach { field ->
            assertEquals(afterMigration[field], afterSave[field], "$field should survive a save")
        }
    }

    @Test
    fun `with both buckets and no choice, nothing takes new files until one is chosen`() {
        mongoTemplate.upsert(
            singleton,
            Update().set("gcpBucketName", "either-gcs").set("s3BucketName", "either-s3").set("s3Region", "eu-west-1"),
            "config"
        )

        StorageStoresFromConfig(crypto).migrate(mongoTemplate)

        val rows = storeRepository.findAll()
        assertEquals(2, rows.size)
        assertFalse(rows.any { it.primary })
        stores.forget()
        val refused = runCatching { storageService.upload("users/u1/photo", null, ByteArray(1)) }.exceptionOrNull()
        assertTrue(refused is IllegalStateException && refused.message!!.contains("choose one"), "was $refused")
    }
}
