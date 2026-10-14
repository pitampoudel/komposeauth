package pitampoudel.komposeauth.storage.entity

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Moving every file of one store to another, and every saved address of them with it.
 *
 * This server has no background queue, so a move is advanced a slice at a time by the Storage page
 * while it is open, and everything a slice needs to carry on from — the cursors as well as the
 * counts — is kept here rather than in a process.
 */
@Document("storage_moves")
@TypeAlias("storage_move")
// One move at a time: a second can't be started while one is [active], on any instance.
@CompoundIndex(name = "one_active_move", def = "{'active': 1}", unique = true, partialFilter = "{'active': true}")
data class StorageMove(
    @Id
    val id: ObjectId = ObjectId(),
    val fromStoreId: ObjectId,
    val toStoreId: ObjectId,
    val fromName: String,
    val toName: String,
    val state: State,
    val active: Boolean = true,
    val startedBy: String? = null,
    /** When every instance has stopped writing to the old store, so reading it can begin. */
    val readNotBefore: Instant,

    val filesTotal: Long = 0,
    val bytesTotal: Long = 0,
    val filesCopied: Long = 0,
    val bytesCopied: Long = 0,
    /** In the new store already with the same size, or written there since the move began. */
    val filesAlreadyThere: Long = 0,
    /** Listed, then deleted before they could be copied. */
    val filesGone: Long = 0,
    val filesFailed: Long = 0,
    /** The first [FAILED_NAMES_KEPT] that failed. */
    val failedNames: List<String> = emptyList(),
    /** The last file name counted or copied; the next slice lists after it. */
    val fileCursor: String? = null,

    /** The collections whose records are read for addresses, fixed when that begins. */
    val collections: List<String> = emptyList(),
    val recordsTotal: Long = 0,
    val recordsRead: Long = 0,
    val linksUpdated: Long = 0,
    /** Inside longer text, under a key an update can't name, or changed while this ran. */
    val linksLeft: Long = 0,
    val collectionIndex: Int = 0,
    /** Which kind of `_id` is being read; see `AddressRewriter.ID_BRACKETS`. */
    val idBracket: Int = 0,
    /** The last `_id` read, as extended JSON so its exact type comes back with it. */
    val lastIdJson: String? = null,

    val error: String? = null,
    val failedSlices: Int = 0,
    val leaseOwner: String? = null,
    val leaseUntil: Instant? = null,
    val startedAt: Instant,
    val updatedAt: Instant,
    val finishedAt: Instant? = null
) {
    enum class State {
        /** New files go to the new store; waiting for every instance to have heard. */
        SWITCHING,
        COUNTING,
        COPYING,
        REWRITING,
        DONE,
        FAILED,
        CANCELLED
    }

    companion object {
        const val FAILED_NAMES_KEPT = 20
    }
}
