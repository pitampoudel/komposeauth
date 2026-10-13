package pitampoudel.komposeauth.core.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GcpStorageServiceTest {

    private val sut = GcpStorageService(GcpStorageService.Settings(projectId = "p", bucket = "b"))

    @Test
    fun `delete returns false for unknown url`() {
        // Ensure we short-circuit before initializing the GCP client.
        assertFalse(sut.delete("https://example.com/not-gcp"))
    }

    @Test
    fun `delete returns false for empty url`() {
        // Ensure we short-circuit before initializing the GCP client.
        assertFalse(sut.delete(""))
    }

    @Test
    fun `owns only addresses in its own bucket`() {
        assertTrue(sut.owns("https://storage.googleapis.com/download/storage/v1/b/b/o/users%2Fu1%2Fphoto?generation=1&alt=media"))
        assertTrue(sut.owns("https://storage.googleapis.com/b/users/u1/photo"))
        assertTrue(sut.owns("gs://b/users/u1/photo"))

        assertFalse(sut.owns("https://storage.googleapis.com/download/storage/v1/b/other/o/users%2Fu1%2Fphoto?alt=media"))
        assertFalse(sut.owns("gs://other/users/u1/photo"))
        assertFalse(sut.delete("gs://other/users/u1/photo"))
    }

    @Test
    fun `an address that only contains one of ours names nothing in the bucket`() {
        assertEquals("users/u1/photo", sut.blobNameOf("https://storage.googleapis.com/b/users/u1/photo"))

        assertNull(sut.blobNameOf("https://evil.example/?next=gs://b/kyc/u2/front"))
        assertNull(sut.blobNameOf("https://evil.example/https://storage.googleapis.com/b/kyc/u2/front"))
        assertNull(
            sut.blobNameOf("https://evil.example/x?https://storage.googleapis.com/download/storage/v1/b/b/o/kyc%2Fu2%2Ffront")
        )
    }
}
