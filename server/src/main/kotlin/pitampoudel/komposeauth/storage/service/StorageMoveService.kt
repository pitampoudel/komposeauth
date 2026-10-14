package pitampoudel.komposeauth.storage.service

import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.FindAndReplaceOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import pitampoudel.komposeauth.core.service.StoredObject
import pitampoudel.komposeauth.storage.entity.StorageMove
import pitampoudel.komposeauth.storage.entity.StorageMove.State
import pitampoudel.komposeauth.storage.repository.StorageMoveRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Moves every file of one store into another and points every saved address at the copies.
 *
 * A move is advanced by [step], a bounded slice at a time, by whoever has the Storage page open:
 * this server has no worker of its own, and a slice small enough to finish inside one request is
 * one that no proxy or host timeout cuts short. A slice holds a lease on the move, so two tabs or
 * two instances stepping at once never run the same work twice; the one without the lease answers
 * the progress and does nothing.
 *
 * The order is what makes it safe to run while people keep uploading:
 * 1. New files go to the target at once, and nothing reads the source until every instance has
 *    picked that up ([StorageMove.readNotBefore]), so no file lands in the source behind the copy.
 * 2. Every file is copied under the same name. Only a copy with no failures goes on.
 * 3. The source's addresses are read from the target from then on, so every address already saved
 *    opens the copy whether or not step 4 reaches it.
 * 4. Every saved address of the source is rewritten to the target's own.
 */
@Service
class StorageMoveService internal constructor(
    private val mongoTemplate: MongoTemplate,
    private val moves: StorageMoveRepository,
    private val stores: StorageStores,
    private val storeService: StorageStoreService,
    private val rewriter: AddressRewriter,
    private val clock: Clock
) {
    @Autowired
    constructor(
        mongoTemplate: MongoTemplate,
        moves: StorageMoveRepository,
        stores: StorageStores,
        storeService: StorageStoreService,
        rewriter: AddressRewriter
    ) : this(mongoTemplate, moves, stores, storeService, rewriter, Clock.systemUTC())

    private val log = LoggerFactory.getLogger(javaClass)

    fun current(): StorageMove? = moves.findFirstByActiveTrue() ?: moves.findFirstByOrderByStartedAtDesc()

    fun start(fromId: ObjectId, toId: ObjectId, startedBy: String?): StorageMove {
        stores.forget()
        val from = stores.byId(fromId) ?: notFound("There is no store to move files from")
        val to = stores.byId(toId) ?: notFound("There is no store to move files to")
        if (fromId == toId) badRequest("Choose two different stores")
        if (to.row.readFromStoreId != null) {
            conflict("${to.row.name}'s own files are read from another store, so files can't be moved into it")
        }
        from.row.readFromStoreId?.takeIf { it != toId }?.let { elsewhere ->
            val name = stores.byId(elsewhere)?.row?.name ?: "another store"
            conflict("${from.row.name}'s files were already copied to $name and are read from there; move them from $name instead")
        }
        val now = clock.instant()
        val move = StorageMove(
            fromStoreId = fromId,
            toStoreId = toId,
            fromName = from.row.name,
            toName = to.row.name,
            state = State.SWITCHING,
            startedBy = startedBy,
            readNotBefore = now + StorageStores.CACHE_TTL + SWITCH_MARGIN,
            startedAt = now,
            updatedAt = now
        )
        val saved = try {
            moves.insert(move)
        } catch (_: DuplicateKeyException) {
            conflict("Files are already being moved; wait for that to finish, or cancel it, first")
        }
        storeService.switchPrimary(toId)
        return saved
    }

    fun cancel(id: ObjectId): StorageMove {
        val now = clock.instant()
        return mongoTemplate.findAndModify(
            Query(Criteria.where("_id").`is`(id).and("active").`is`(true)),
            Update()
                .set("active", false)
                .set("state", State.CANCELLED)
                .set("finishedAt", now)
                .set("updatedAt", now)
                .unset("leaseOwner")
                .unset("leaseUntil"),
            FindAndModifyOptions.options().returnNew(true),
            StorageMove::class.java
        ) ?: moves.findById(id).orElse(null)?.let { conflict("That move has already ended") }
        ?: notFound("There is no such move")
    }

    /**
     * Does up to [budget] of the move's work and answers where it stands. Answers it unchanged when
     * the move has ended, when another slice holds it, or while the switch is still settling.
     */
    fun step(id: ObjectId, budget: Duration = SLICE_BUDGET): StorageMove {
        val move = moves.findById(id).orElse(null) ?: notFound("There is no such move")
        if (!move.active) return move
        if (move.state == State.SWITCHING && clock.instant() < move.readNotBefore) return move

        val token = UUID.randomUUID().toString()
        val leased = lease(id, token) ?: return move
        val deadline = clock.instant() + budget
        val slice = Slice(token, leased)
        val outcome = try {
            slice.run(deadline)
            slice.move.let { if (it.active) it.copy(failedSlices = 0, error = null) else it }
        } catch (ex: Exception) {
            log.warn("A slice of storage move {} failed", id, ex)
            val failedSlices = slice.move.failedSlices + 1
            val message = ex.message?.take(400) ?: ex.javaClass.simpleName
            if (failedSlices >= SLICES_BEFORE_GIVING_UP) {
                slice.move.ended(State.FAILED, "Stopped after $failedSlices failures in a row. The last: $message")
            } else {
                slice.move.copy(failedSlices = failedSlices, error = message)
            }
        }
        return save(outcome.copy(leaseOwner = null, leaseUntil = null, updatedAt = clock.instant()), token)
            ?: moves.findById(id).orElse(move)
    }

    /** One slice under one lease. [move] is the move as this slice has advanced it. */
    private inner class Slice(val token: String, var move: StorageMove) {
        private var leaseRenewedAt = clock.instant()

        fun run(deadline: Instant) {
            while (move.active && clock.instant() < deadline) {
                keepLease()
                when (move.state) {
                    State.SWITCHING -> move = move.copy(state = State.COUNTING, fileCursor = null)
                    State.COUNTING -> count()
                    State.COPYING -> copy(deadline)
                    State.REWRITING -> rewrite(deadline)
                    State.DONE, State.FAILED, State.CANCELLED -> return
                }
            }
        }

        private fun count() {
            val page = source().list(after = move.fileCursor, limit = LIST_PAGE)
            move = move.copy(
                filesTotal = move.filesTotal + page.size,
                bytesTotal = move.bytesTotal + page.sumOf { it.size },
                fileCursor = page.lastOrNull()?.name ?: move.fileCursor
            )
            if (page.size < LIST_PAGE) move = move.copy(state = State.COPYING, fileCursor = null)
        }

        private fun copy(deadline: Instant) {
            val page = source().list(after = move.fileCursor, limit = COPY_PAGE)
            for (file in page) {
                if (clock.instant() >= deadline) return
                keepLease()
                move = copyOne(file).copy(fileCursor = file.name)
            }
            if (page.size < COPY_PAGE) finishCopying()
        }

        private fun copyOne(file: StoredObject): StorageMove {
            val counted = move.copy(filesTotal = maxOf(move.filesTotal, done(move) + 1))
            return try {
                val target = target()
                val there = target.stat(file.name)
                // The same size is taken as the same file. One written since the move began is the
                // newer of the two, since new files have been going to the target since then.
                if (there != null && (there.size == file.size || there.updatedAt?.isAfter(move.startedAt - CLOCK_SKEW) == true)) {
                    return counted.copy(filesAlreadyThere = counted.filesAlreadyThere + 1)
                }
                val copied = source().open(file.name) { opened ->
                    target.write(file.name, opened.contentType, opened.size, opened.content)
                    opened.size
                }
                if (copied == null) counted.copy(filesGone = counted.filesGone + 1)
                else counted.copy(filesCopied = counted.filesCopied + 1, bytesCopied = counted.bytesCopied + copied)
            } catch (ex: Exception) {
                log.warn("Could not copy {} from {} to {}", file.name, move.fromName, move.toName, ex)
                counted.copy(
                    filesFailed = counted.filesFailed + 1,
                    failedNames = (counted.failedNames + file.name).take(StorageMove.FAILED_NAMES_KEPT)
                )
            }
        }

        private fun finishCopying() {
            if (move.filesFailed > 0) {
                move = move.ended(
                    State.FAILED,
                    "${move.filesFailed} of ${move.filesTotal} files could not be copied, so the saved " +
                        "addresses were left as they are and still open ${move.fromName}'s files. Put right " +
                        "what stopped them, then move again: files already copied are skipped."
                )
                return
            }
            // From here on the slice changes what other code reads, so a cancel has to have stopped it.
            keepLease(force = true)
            storeService.readFrom(move.fromStoreId, move.toStoreId)
            val collections = rewriter.collections()
            move = move.copy(
                state = State.REWRITING,
                fileCursor = null,
                collections = collections,
                recordsTotal = rewriter.recordsIn(collections),
                collectionIndex = 0,
                idBracket = 0,
                lastIdJson = null
            )
        }

        private fun rewrite(deadline: Instant) {
            stores.forget()
            val entries = stores.all()
            val target = stores.byId(move.toStoreId) ?: error("${move.toName} is no longer listed")
            // The source's own addresses, and those of any store whose files were in the source.
            val holders = entries.filter { entry -> chainOf(entry.row.id, entries).contains(move.fromStoreId) }
            val rewrite = { value: String ->
                holders.firstNotNullOfOrNull { it.addresses.nameOf(value) }?.let(target.addresses::addressOf)
            }
            val mentions = { value: String -> holders.any { it.addresses.mentionedIn(value) } }

            var position = AddressRewriter.Position(move.collectionIndex, move.idBracket, move.lastIdJson)
            while (clock.instant() < deadline) {
                keepLease(force = true)
                val batch = rewriter.rewriteBatch(move.collections, position, REWRITE_PAGE, rewrite, mentions)
                position = batch.position
                val read = move.recordsRead + batch.recordsRead
                move = move.copy(
                    collectionIndex = position.collectionIndex,
                    idBracket = position.idBracket,
                    lastIdJson = position.lastIdJson,
                    recordsRead = read,
                    recordsTotal = maxOf(move.recordsTotal, read),
                    linksUpdated = move.linksUpdated + batch.linksUpdated,
                    linksLeft = move.linksLeft + batch.linksLeft
                )
                if (batch.finished) {
                    move = move.ended(State.DONE, null).copy(recordsTotal = move.recordsRead)
                    return
                }
            }
        }

        private fun source() = (stores.byId(move.fromStoreId) ?: error("${move.fromName} is no longer listed")).store

        private fun target() = (stores.byId(move.toStoreId) ?: error("${move.toName} is no longer listed")).store

        /**
         * Pushes the lease out again, and stops the slice if a cancel or another slice took it. Only
         * every so often, unless [force]d before a step that writes outside the move itself.
         */
        private fun keepLease(force: Boolean = false) {
            val now = clock.instant()
            if (!force && Duration.between(leaseRenewedAt, now) < LEASE.dividedBy(3)) return
            val renewed = mongoTemplate.updateFirst(
                Query(Criteria.where("_id").`is`(move.id).and("active").`is`(true).and("leaseOwner").`is`(token)),
                Update().set("leaseUntil", now + LEASE),
                StorageMove::class.java
            )
            check(renewed.matchedCount == 1L) { "The move was cancelled or taken over by another slice" }
            leaseRenewedAt = now
        }
    }

    /** The ids [id] reads its files through: itself, then each store it was copied to in turn. */
    private fun chainOf(id: ObjectId, entries: List<StorageStores.Entry>): List<ObjectId> {
        val chain = mutableListOf(id)
        var next = entries.firstOrNull { it.row.id == id }?.row?.readFromStoreId
        while (next != null && next !in chain) {
            chain += next
            next = entries.firstOrNull { it.row.id == next }?.row?.readFromStoreId
        }
        return chain
    }

    private fun lease(id: ObjectId, token: String): StorageMove? {
        val now = clock.instant()
        return mongoTemplate.findAndModify(
            Query(
                Criteria.where("_id").`is`(id).and("active").`is`(true)
                    .orOperator(Criteria.where("leaseUntil").`is`(null), Criteria.where("leaseUntil").lt(now))
            ),
            Update().set("leaseOwner", token).set("leaseUntil", now + LEASE),
            FindAndModifyOptions.options().returnNew(true),
            StorageMove::class.java
        )
    }

    /** Saves what the slice did, unless a cancel or another slice has taken the move since. */
    private fun save(move: StorageMove, token: String): StorageMove? =
        mongoTemplate.findAndReplace(
            Query(Criteria.where("_id").`is`(move.id).and("active").`is`(true).and("leaseOwner").`is`(token)),
            move,
            FindAndReplaceOptions.options().returnNew()
        )

    private fun StorageMove.ended(state: State, error: String?) =
        copy(state = state, active = false, error = error, finishedAt = clock.instant())

    companion object {
        /** About the longest a slice may run, well inside any request timeout in front of this server. */
        val SLICE_BUDGET: Duration = Duration.ofSeconds(8)

        /** How long a slice holds the move before another may take it, should this one die mid-way. */
        val LEASE: Duration = Duration.ofSeconds(60)

        /** Beyond the cache's own TTL, for an upload already under way when the switch was seen. */
        val SWITCH_MARGIN: Duration = Duration.ofSeconds(10)

        /** How far the target's clock may be behind ours when telling a file written since the move began. */
        val CLOCK_SKEW: Duration = Duration.ofMinutes(1)

        const val SLICES_BEFORE_GIVING_UP = 5
        const val LIST_PAGE = 1000
        const val COPY_PAGE = 100
        const val REWRITE_PAGE = 200

        /** Every file the copy has dealt with, one way or another. */
        fun done(move: StorageMove): Long = move.filesCopied + move.filesAlreadyThere + move.filesGone + move.filesFailed

        private fun badRequest(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)
        private fun conflict(message: String): Nothing = throw ResponseStatusException(HttpStatus.CONFLICT, message)
        private fun notFound(message: String): Nothing = throw ResponseStatusException(HttpStatus.NOT_FOUND, message)
    }
}
