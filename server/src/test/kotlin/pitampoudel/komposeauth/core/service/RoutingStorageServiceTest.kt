package pitampoudel.komposeauth.core.service

import org.bson.types.ObjectId
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.storage.InMemoryBucket
import pitampoudel.komposeauth.storage.entity.StorageStore
import pitampoudel.komposeauth.storage.repository.StorageStoreRepository
import pitampoudel.komposeauth.storage.service.BucketStoreFactory
import pitampoudel.komposeauth.storage.service.StorageStores
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoutingStorageServiceTest {
    private var rows: List<StorageStore> = emptyList()
    private val repository: StorageStoreRepository = mock { on { findAll() } doAnswer { rows } }
    private val crypto: CryptoService = mock { on { decrypt(any()) } doAnswer { it.getArgument(0) } }

    private val buckets = mutableMapOf<ObjectId, InMemoryBucket>()
    /** Every client built, by store, so a test can see which were never needed. */
    private val built = mutableListOf<ObjectId>()
    private val stores = StorageStores(repository, crypto, BucketStoreFactory { row ->
        built += row.id
        bucketOf(row)
    })
    private val router = RoutingStorageService(stores)

    private val old = StorageStore(name = "Old", kind = StorageStore.Kind.GCS, bucket = "old", createdAt = Instant.EPOCH)
    private val new = StorageStore(
        name = "New", kind = StorageStore.Kind.S3, bucket = "new-files", region = "ap-south-1", primary = true
    )
    private val gcsUrl = "https://storage.googleapis.com/download/storage/v1/b/old/o/users%2Fu1%2Fphoto?alt=media"

    private fun bucketOf(row: StorageStore) = buckets.getOrPut(row.id) { InMemoryBucket(row.addresses()) }

    private fun list(vararg stores: StorageStore) {
        rows = stores.toList()
        this.stores.forget()
    }

    @Test
    fun `new files go to the store that takes them`() {
        list(old, new)
        assertEquals(
            "https://new-files.s3.ap-south-1.amazonaws.com/users/u1/photo",
            router.upload("users/u1/photo", "image/png", "photo".toByteArray())
        )
        assertEquals("photo", bucketOf(new).text("users/u1/photo"))
        assertEquals(listOf(new.id), built, "nothing else needed a client")
    }

    @Test
    fun `with no store, or none taking new files, an upload is refused saying what to do`() {
        list()
        assertContains(assertFailsWith<IllegalStateException> { router.upload("a", null, ByteArray(1)) }.message!!, "add a bucket")
        list(old)
        assertContains(assertFailsWith<IllegalStateException> { router.upload("a", null, ByteArray(1)) }.message!!, "choose one")
    }

    @Test
    fun `a read and a delete go to the store the address is in, not the one new files go to`() {
        list(old, new)
        bucketOf(old).put("users/u1/photo", "stored")

        assertEquals("users/u1/photo", router.blobNameOf(gcsUrl))
        assertEquals("stored", router.download(gcsUrl)?.decodeToString())
        assertTrue(router.delete(gcsUrl))
        assertNull(bucketOf(old).text("users/u1/photo"))
        assertNull(router.download(gcsUrl), "an address of a listed store with nothing behind it is an absent file")
    }

    @Test
    fun `an address no store holds is not an absent file`() {
        list(old, new)
        assertFailsWith<IllegalStateException> { router.download("https://example.com/elsewhere") }
        assertFalse(router.delete("https://example.com/elsewhere"))
        assertNull(router.blobNameOf("https://example.com/elsewhere"))
        assertNull(router.blobNameOf("https://evil.example/?https://new-files.s3.ap-south-1.amazonaws.com/users/u1/photo"))
    }

    @Test
    fun `a store whose files were copied elsewhere answers its addresses from there, with no client of its own`() {
        list(old.copy(readFromStoreId = new.id), new)
        bucketOf(new).put("users/u1/photo", "copied")

        assertEquals("users/u1/photo", router.blobNameOf(gcsUrl))
        assertEquals("copied", router.download(gcsUrl)?.decodeToString())
        assertTrue(router.delete(gcsUrl))
        assertNull(bucketOf(new).text("users/u1/photo"))
        assertFalse(old.id in built, "the old store's cloud was never signed in to")
    }

    @Test
    fun `copies are followed one after another`() {
        val newest = StorageStore(name = "Newest", kind = StorageStore.Kind.S3, bucket = "newest", region = "auto",
            endpoint = "https://minio.internal:9000", primary = true)
        list(old.copy(readFromStoreId = new.id), new.copy(primary = false, readFromStoreId = newest.id), newest)
        bucketOf(newest).put("users/u1/photo", "twice copied")

        assertEquals("twice copied", router.download(gcsUrl)?.decodeToString())
        assertEquals("twice copied", router.download("https://new-files.s3.ap-south-1.amazonaws.com/users/u1/photo")?.decodeToString())
        assertEquals(listOf(newest.id), built)
    }

    @Test
    fun `a client is built once, and again only when how to reach the bucket changes`() {
        list(new)
        router.upload("a", null, ByteArray(1))
        list(new.copy(name = "Renamed"))
        router.upload("b", null, ByteArray(1))
        assertEquals(1, built.size, "a new name is not a new connection")

        list(new.copy(accessKeyId = "AKIA", secretAccessKey = "secret"))
        router.upload("c", null, ByteArray(1))
        assertEquals(2, built.size, "new credentials are")
    }
}
