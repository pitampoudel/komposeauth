package pitampoudel.komposeauth.storage.service

import kotlin.test.Test
import kotlin.test.assertEquals

class UnsharedFilesTest {

    /** Holds files named by address `bucket://<name>`, and can be told to fail. */
    private class Bucket : StorageService {
        val files = mutableSetOf<String>()
        var unreachable = false

        override fun upload(blobName: String, contentType: String?, bytes: ByteArray) =
            "bucket://$blobName".also { files += blobName }

        override fun delete(url: String): Boolean {
            check(!unreachable) { "unreachable" }
            files.remove(url.removePrefix("bucket://"))
            return true
        }

        override fun blobNameOf(url: String): String? {
            check(!unreachable) { "unreachable" }
            // An older address of the same file, with a query, names it too.
            return url.takeIf { it.startsWith("bucket://") }?.removePrefix("bucket://")?.substringBefore('?')
        }

        override fun download(url: String): ByteArray? = null
    }

    @Test
    fun `a file only deleted rows name is deleted, and one a kept row names stays`() {
        val bucket = Bucket()
        val gone = bucket.upload("kyc/a/front", null, byteArrayOf())
        val shared = bucket.upload("kyc/b/front", null, byteArrayOf())

        val left = bucket.deleteUnsharedFiles(listOf(gone, "$shared?generation=1"), kept = listOf(shared))

        assertEquals(emptyList(), left)
        assertEquals(setOf("kyc/b/front"), bucket.files)
    }

    @Test
    fun `an address in no listed bucket is left and answered`() {
        val bucket = Bucket()
        val elsewhere = "https://elsewhere.example/a.png"

        assertEquals(listOf(elsewhere), bucket.deleteUnsharedFiles(listOf(elsewhere), kept = emptyList()))
    }

    @Test
    fun `a bucket that can't be reached leaves every file and throws nothing`() {
        val bucket = Bucket()
        val file = bucket.upload("organization_logos/x", null, byteArrayOf())
        bucket.unreachable = true

        assertEquals(listOf(file), bucket.deleteUnsharedFiles(listOf(file, file), kept = listOf("bucket://other")))
        assertEquals(listOf(file), bucket.deleteUnsharedFiles(listOf(file), kept = emptyList()))
    }
}
