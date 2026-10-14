package pitampoudel.komposeauth.kyc.service

import org.bson.types.ObjectId
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import pitampoudel.komposeauth.core.domain.ApiEndpoints
import pitampoudel.komposeauth.storage.service.StorageService
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.core.utils.ServerUrl
import pitampoudel.komposeauth.kyc.entity.KycDocument

/**
 * The identity document scans and the selfie of a KYC submission.
 *
 * They go to the same bucket as profile photos, and anyone holding an address in that bucket can
 * read it, which is what a profile photo is for. So a document is sealed with this server's
 * encryption key before it is uploaded: the stored copy is of no use to whoever holds or guesses its
 * address, and the bucket and its permissions stay as they are. Only this server opens it again, for
 * the person it belongs to and for KYC reviewers, at [addressOf]. The bucket address stays on the
 * record and is never handed out.
 */
@Component
class KycDocuments(
    private val storageService: StorageService,
    private val cryptoService: CryptoService,
    private val serverUrl: ServerUrl
) {

    companion object {
        /** Where a document is read back, signed in as its owner or as a reviewer. */
        const val PATH = "/${ApiEndpoints.KYC}/{userId}/documents/{document}"
    }

    sealed interface Sealing {
        /** Sealed in place; the record should now name [storedUrl]. */
        data class Sealed(val storedUrl: String) : Sealing
        data object AlreadySealed : Sealing
        data object Missing : Sealing
    }

    /** Seals [bytes] and stores them, answering the storage address to keep on the record. */
    fun store(userId: ObjectId, document: KycDocument, bytes: ByteArray): String {
        val name = blobName(userId, document)
        return storageService.upload(name, MediaType.APPLICATION_OCTET_STREAM_VALUE, cryptoService.seal(bytes, name))
    }

    /** The document as it was submitted, or null when the store has nothing at [storedUrl]. */
    fun read(userId: ObjectId, document: KycDocument, storedUrl: String): ByteArray? {
        val stored = storageService.download(storedUrl) ?: return null
        // A file stored before documents were sealed, which KycDocumentSealing has not reached yet.
        if (!cryptoService.isSealed(stored)) return stored
        return cryptoService.open(stored, blobName(userId, document))
    }

    /** Seals a file that was stored as it was submitted, under the same name. */
    fun sealStored(userId: ObjectId, document: KycDocument, storedUrl: String): Sealing {
        val stored = storageService.download(storedUrl) ?: return Sealing.Missing
        if (cryptoService.isSealed(stored)) return Sealing.AlreadySealed
        return Sealing.Sealed(store(userId, document, stored))
    }

    /** The address a document is handed out under: this server's, never the bucket's. */
    fun addressOf(userId: ObjectId, document: KycDocument): String =
        UriComponentsBuilder.fromUriString(serverUrl.value)
            .path(PATH)
            .buildAndExpand(userId.toHexString(), document.label)
            .toUriString()

    /** Also the context a document is sealed under, so a sealed file opens only as the one it was. */
    private fun blobName(userId: ObjectId, document: KycDocument) = "kyc/${userId.toHexString()}/${document.label}"
}
