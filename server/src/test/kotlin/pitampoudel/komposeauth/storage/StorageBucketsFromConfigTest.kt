package pitampoudel.komposeauth.storage

import org.bson.Document
import pitampoudel.komposeauth.storage.entity.StorageBucket.Kind
import pitampoudel.komposeauth.storage.migration.StorageBucketsFromConfig.Companion.bucketsFrom
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the previous release's two bucket slots become on the Storage page. */
class StorageBucketsFromConfigTest {
    private val now = Instant.parse("2026-10-01T00:00:00Z")

    private fun config(vararg fields: Pair<String, String>) = Document(fields.toMap())

    private val gcs = arrayOf("gcpBucketName" to "old-files", "gcpProjectId" to "my-project")
    private val s3 = arrayOf(
        "s3BucketName" to "new-files", "s3Region" to "ap-south-1",
        "s3AccessKeyId" to "AKIA", "s3SecretAccessKey" to "enc:stored-ciphertext"
    )

    @Test
    fun `with nothing configured nothing is listed`() {
        assertEquals(emptyList(), bucketsFrom(config(), now))
        assertEquals(emptyList(), bucketsFrom(config("gcpBucketName" to " ", "storageProvider" to "gcs"), now))
    }

    @Test
    fun `the only bucket configured takes new files, as it did`() {
        val onlyGcs = bucketsFrom(config(*gcs), now).single()
        assertEquals(Kind.GCS, onlyGcs.kind)
        assertEquals("old-files", onlyGcs.bucketName)
        assertEquals("my-project", onlyGcs.projectId)
        assertTrue(onlyGcs.primary)
        assertTrue(onlyGcs.usesHostIdentity())

        val onlyS3 = bucketsFrom(config(*s3), now).single()
        assertEquals(Kind.S3, onlyS3.kind)
        assertEquals("ap-south-1", onlyS3.region)
        assertEquals("AKIA", onlyS3.accessKeyId)
        assertEquals("enc:stored-ciphertext", onlyS3.secretAccessKey, "the secret is carried as it was stored")
        assertTrue(onlyS3.primary)
    }

    @Test
    fun `with both, storageProvider decides which takes new files`() {
        val toS3 = bucketsFrom(config(*gcs, *s3, "storageProvider" to "s3"), now)
        assertEquals(listOf(Kind.GCS to false, Kind.S3 to true), toS3.map { it.kind to it.primary })

        val toGcs = bucketsFrom(config(*gcs, *s3, "storageProvider" to "GCS"), now)
        assertEquals(listOf(Kind.GCS to true, Kind.S3 to false), toGcs.map { it.kind to it.primary })
    }

    @Test
    fun `with both and no choice neither takes new files, as neither did`() {
        val rows = bucketsFrom(config(*gcs, *s3), now)
        assertEquals(2, rows.size)
        assertTrue(rows.none { it.primary })
    }

    @Test
    fun `a choice of a bucket that isn't configured chooses nothing`() {
        assertTrue(bucketsFrom(config(*gcs, "storageProvider" to "s3"), now).none { it.primary })
    }

    @Test
    fun `once the GCS files were copied to S3, GCS reads them from there and S3 takes new files`() {
        val (old, new) = bucketsFrom(config(*gcs, *s3, "gcsFilesReadFrom" to "s3"), now)
        assertEquals(new.id, old.readFromBucketId)
        assertTrue(new.primary)
        assertTrue(!old.primary)
        assertNull(new.readFromBucketId)
        assertTrue(old.createdAt < new.createdAt, "the older bucket is listed first")

        val named = bucketsFrom(config(*gcs, *s3, "gcsFilesReadFrom" to "s3", "storageProvider" to "gcs"), now)
        assertTrue(named.none { it.primary }, "GCS can't take new files once its files are read from S3")
    }

    @Test
    fun `gcsFilesReadFrom without an S3 bucket is not a copy`() {
        val only = bucketsFrom(config(*gcs, "gcsFilesReadFrom" to "s3"), now).single()
        assertNull(only.readFromBucketId)
        assertTrue(only.primary)
    }

    @Test
    fun `an S3 bucket with no region can't be listed`() {
        val rows = bucketsFrom(config(*gcs, "s3BucketName" to "new-files", "storageProvider" to "s3"), now)
        assertEquals(listOf(Kind.GCS), rows.map { it.kind })
        assertTrue(rows.none { it.primary })
    }
}
