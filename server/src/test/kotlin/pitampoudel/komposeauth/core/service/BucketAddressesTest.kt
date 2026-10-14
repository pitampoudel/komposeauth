package pitampoudel.komposeauth.core.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BucketAddressesTest {

    private val gcs = GcsAddresses("b")
    private val aws = S3Addresses(bucket = "new-files", region = "ap-south-1")
    private val r2 = S3Addresses(bucket = "files", region = "auto", endpoint = "https://acc.r2.cloudflarestorage.com")
    private val behindCdn = S3Addresses(
        bucket = "files", region = "auto", endpoint = "https://acc.r2.cloudflarestorage.com", publicBaseUrl = "https://cdn.example.com"
    )

    @Test
    fun `GCS answers its media links, its direct addresses and gs addresses`() {
        assertEquals(
            "users/u1/photo",
            gcs.nameOf("https://storage.googleapis.com/download/storage/v1/b/b/o/users%2Fu1%2Fphoto?generation=1&alt=media")
        )
        assertEquals("users/u1/photo", gcs.nameOf("https://storage.googleapis.com/b/users/u1/photo"))
        assertEquals("users/u1/photo", gcs.nameOf("gs://b/users/u1/photo"))
        assertEquals(
            "https://storage.googleapis.com/download/storage/v1/b/b/o/users%2Fu1%2Fphoto?alt=media",
            gcs.addressOf("users/u1/photo")
        )
    }

    @Test
    fun `GCS refuses another bucket's addresses and one that only contains its own`() {
        assertNull(gcs.nameOf("https://storage.googleapis.com/download/storage/v1/b/other/o/users%2Fu1%2Fphoto?alt=media"))
        assertNull(gcs.nameOf("gs://other/users/u1/photo"))
        assertNull(gcs.nameOf("https://example.com/not-gcp"))
        assertNull(gcs.nameOf(""))

        assertNull(gcs.nameOf("https://evil.example/?next=gs://b/kyc/u2/front"))
        assertNull(gcs.nameOf("https://evil.example/https://storage.googleapis.com/b/kyc/u2/front"))
        assertNull(gcs.nameOf("https://evil.example/x?https://storage.googleapis.com/download/storage/v1/b/b/o/kyc%2Fu2%2Ffront"))
        assertNull(gcs.nameOf("see gs://b/kyc/u2/front"))
    }

    @Test
    fun `S3 on AWS answers its own bucket's addresses, version and all`() {
        assertEquals("users/u1/photo", aws.nameOf("https://new-files.s3.ap-south-1.amazonaws.com/users/u1/photo?v=1700000000000"))
        assertEquals("https://new-files.s3.ap-south-1.amazonaws.com/users/u1/photo", aws.addressOf("users/u1/photo"))

        assertNull(aws.nameOf("https://other.s3.ap-south-1.amazonaws.com/users/u1/photo"))
        assertNull(aws.nameOf("https://new-files.s3.us-east-1.amazonaws.com/users/u1/photo"))
        assertNull(aws.nameOf(gcs.addressOf("users/u1/photo")))
        assertNull(aws.nameOf("https://evil.example/?https://new-files.s3.ap-south-1.amazonaws.com/users/u1/photo"))
    }

    @Test
    fun `S3 anywhere else is addressed path-style under its endpoint`() {
        assertEquals("https://acc.r2.cloudflarestorage.com/files/users/u1/photo", r2.addressOf("users/u1/photo"))
        assertEquals("users/u1/photo", r2.nameOf("https://acc.r2.cloudflarestorage.com/files/users/u1/photo?v=1"))

        assertNull(r2.nameOf("https://acc.r2.cloudflarestorage.com/other/users/u1/photo"))
        assertNull(r2.nameOf("https://acc.r2.cloudflarestorage.com/files"))
        assertNull(r2.nameOf("https://files.s3.auto.amazonaws.com/users/u1/photo"))
    }

    @Test
    fun `behind a public address new files are handed out under it, and the bucket's own addresses still read`() {
        assertEquals("https://cdn.example.com/users/u1/photo", behindCdn.addressOf("users/u1/photo"))
        assertEquals("users/u1/photo", behindCdn.nameOf("https://cdn.example.com/users/u1/photo?v=2"))
        assertEquals("users/u1/photo", behindCdn.nameOf("https://acc.r2.cloudflarestorage.com/files/users/u1/photo"))
        assertNull(behindCdn.nameOf("https://cdn.example.com.evil.example/users/u1/photo"))

        val gcsBehindCdn = GcsAddresses("b", "https://files.example.com/media")
        assertEquals("https://files.example.com/media/users/u1/photo", gcsBehindCdn.addressOf("users/u1/photo"))
        assertEquals("users/u1/photo", gcsBehindCdn.nameOf("https://files.example.com/media/users/u1/photo"))
        assertEquals("users/u1/photo", gcsBehindCdn.nameOf("gs://b/users/u1/photo"))
        assertNull(gcsBehindCdn.nameOf("https://files.example.com/users/u1/photo"))
    }

    @Test
    fun `a name that climbs out of its folder is refused, however it is spelled`() {
        listOf<BucketAddresses>(aws, r2, behindCdn).forEach { addresses ->
            val base = addresses.addressOf("users/u1/photo").removeSuffix("users/u1/photo")
            assertNull(addresses.nameOf("${base}users/../kyc/u2/front"), base)
            assertNull(addresses.nameOf("${base}users/%2E%2E/kyc/u2/front"), base)
            assertNull(addresses.nameOf("${base}users%2F..%2Fkyc%2Fu2%2Ffront"), base)
            assertNull(addresses.nameOf("${base}./users/u1/photo"), base)
            assertNull(addresses.nameOf("${base}users/%zz"), "$base: a malformed escape names nothing")
        }
        assertNull(gcs.nameOf("gs://b/users/../kyc/u2/front"))
        assertNull(gcs.nameOf("https://storage.googleapis.com/b/users/../kyc/u2/front"))
        assertNull(gcs.nameOf("https://storage.googleapis.com/download/storage/v1/b/b/o/users%2F..%2Fkyc%2Fu2%2Ffront?alt=media"))
        assertNull(gcs.nameOf("https://storage.googleapis.com/download/storage/v1/b/b/o/users%zz?alt=media"))
    }

    @Test
    fun `a name is written so it reads back the same`() {
        val name = "users/u 1/ph+oto é"
        listOf(gcs, aws, r2, behindCdn, GcsAddresses("b", "https://files.example.com")).forEach { addresses ->
            val address = addresses.addressOf(name)
            assertFalse(address.any(Char::isWhitespace), address)
            assertEquals(name, addresses.nameOf(address), address)
            addresses.formsOf(name).filter { !it.any(Char::isWhitespace) }.forEach { form ->
                assertEquals(name, addresses.nameOf(form), form)
            }
        }
    }

    @Test
    fun `an address is found inside longer text, which is how a move counts what it leaves`() {
        assertTrue(aws.mentionedIn("see https://new-files.s3.ap-south-1.amazonaws.com/users/u1/photo for it"))
        assertTrue(gcs.mentionedIn("""{"url": "gs://b/users/u1/photo"}"""))
        assertTrue(behindCdn.mentionedIn("<img src=\"https://cdn.example.com/users/u1/photo\">"))
        assertFalse(aws.mentionedIn("https://other.s3.ap-south-1.amazonaws.com/users/u1/photo"))
        assertFalse(gcs.mentionedIn("gs://bb/users/u1/photo"))
    }

    @Test
    fun `two stores answering one bucket's addresses overlap, and two buckets don't`() {
        assertTrue(BucketAddresses.overlap(aws, S3Addresses("new-files", "ap-south-1", publicBaseUrl = "https://cdn.example.com")))
        assertTrue(BucketAddresses.overlap(behindCdn, S3Addresses("elsewhere", "us-east-1", publicBaseUrl = "https://cdn.example.com")))
        assertTrue(BucketAddresses.overlap(r2, S3Addresses("x", "auto", publicBaseUrl = "https://acc.r2.cloudflarestorage.com/files")))
        assertTrue(BucketAddresses.overlap(gcs, GcsAddresses("b", "https://files.example.com")))

        assertFalse(BucketAddresses.overlap(gcs, GcsAddresses("c")))
        assertFalse(BucketAddresses.overlap(aws, S3Addresses("new-files", "us-east-1")))
        assertFalse(BucketAddresses.overlap(r2, S3Addresses("files2", "auto", endpoint = "https://acc.r2.cloudflarestorage.com")))
        assertFalse(BucketAddresses.overlap(gcs, aws))
    }
}
