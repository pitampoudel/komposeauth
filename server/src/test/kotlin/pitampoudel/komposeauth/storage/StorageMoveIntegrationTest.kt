package pitampoudel.komposeauth.storage

import org.bson.Document
import org.bson.types.ObjectId
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
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.server.ResponseStatusException
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.core.service.RoutingStorageService
import pitampoudel.komposeauth.core.service.security.CryptoService
import pitampoudel.komposeauth.storage.dto.CreateStoreRequest
import pitampoudel.komposeauth.storage.entity.StorageMove
import pitampoudel.komposeauth.storage.entity.StorageMove.State
import pitampoudel.komposeauth.storage.entity.StorageStore
import pitampoudel.komposeauth.storage.entity.StorageStore.Kind
import pitampoudel.komposeauth.storage.repository.StorageMoveRepository
import pitampoudel.komposeauth.storage.repository.StorageStoreRepository
import pitampoudel.komposeauth.storage.service.AddressRewriter
import pitampoudel.komposeauth.storage.service.BucketStoreFactory
import pitampoudel.komposeauth.storage.service.StorageMoveService
import pitampoudel.komposeauth.storage.service.StorageStoreService
import pitampoudel.komposeauth.storage.service.StorageStores
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A move against a real Mongo, with the buckets kept in memory: the copy, the rewrite of every saved
 * address, and the lease that keeps two slices from running the same work.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class StorageMoveIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var storeRepository: StorageStoreRepository

    @Autowired
    private lateinit var moveRepository: StorageMoveRepository

    @Autowired
    private lateinit var crypto: CryptoService

    /** Stands still unless moved, or moves on by [tick] every time it is read. */
    private class TestClock(var now: Instant, private val tick: Duration = Duration.ZERO) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now.also { now = now.plus(tick) }
    }

    private val buckets = mutableMapOf<ObjectId, InMemoryBucket>()
    private lateinit var clock: TestClock
    private lateinit var stores: StorageStores
    private lateinit var storeService: StorageStoreService
    private lateinit var rewriter: AddressRewriter
    private lateinit var moves: StorageMoveService

    private val start = Instant.parse("2026-10-01T00:00:00Z")

    private fun services(clock: TestClock = TestClock(start)) {
        this.clock = clock
        stores = StorageStores(storeRepository, crypto, BucketStoreFactory { row -> bucketOf(row) })
        storeService = StorageStoreService(storeRepository, moveRepository, mongoTemplate, stores, crypto)
        rewriter = AddressRewriter(mongoTemplate)
        moves = StorageMoveService(mongoTemplate, moveRepository, stores, storeService, rewriter, clock)
    }

    private fun bucketOf(row: StorageStore) = buckets.getOrPut(row.id) { InMemoryBucket(row.addresses()) { clock.now } }

    private fun store(name: String, bucket: String, kind: Kind = Kind.S3, publicBaseUrl: String? = null) =
        storeService.create(
            CreateStoreRequest(
                name = name,
                kind = kind,
                bucket = bucket,
                region = if (kind == Kind.S3) "ap-south-1" else null,
                publicBaseUrl = publicBaseUrl
            )
        )

    private fun row(store: StorageStore) = storeRepository.findById(store.id).get()

    private fun record(id: Any): Document = mongoTemplate.getCollection(RECORDS).find(Document("_id", id)).first()!!

    private fun runToEnd(move: StorageMove, limit: Int = 500): Pair<StorageMove, Int> {
        var current = move
        var steps = 0
        while (current.active) {
            check(steps++ < limit) { "The move did not end in $limit steps: $current" }
            current = moves.step(current.id)
        }
        return current to steps
    }

    @BeforeEach
    @AfterEach
    fun clean() {
        storeRepository.deleteAll()
        moveRepository.deleteAll()
        mongoTemplate.dropCollection(RECORDS)
        buckets.clear()
    }

    @Test
    fun `a move copies every file and points every saved address at the copy, leaving what it can't rewrite`() {
        services()
        val old = store("Old", "move-old-files")
        val new = store("New", "move-new-files", Kind.GCS, publicBaseUrl = "https://cdn.example.com")
        val from = bucketOf(old)
        val to = bucketOf(new)
        from.put("users/u1/photo", "photo-1")
        from.put("kyc/u1/front", "sealed-front")
        from.put("organization_logos/a", "logo")
        from.put("users/u2/photo", "photo-2")
        from.put("users/u3/photo", "old-3")
        from.put("users/u4/photo", "photo-4")
        to.put("users/u2/photo", "photo-2", updatedAt = start.minus(Duration.ofDays(1)))
        to.put("users/u3/photo", "written since the move began", updatedAt = start.plus(Duration.ofMinutes(5)))
        to.put("users/u4/photo", "stale", updatedAt = start.minus(Duration.ofDays(1)))

        val oldAddress = { name: String -> old.addresses().addressOf(name) }
        val newAddress = { name: String -> new.addresses().addressOf(name) }
        val photoRecord = ObjectId()
        val untouched = ObjectId()
        mongoTemplate.getCollection(RECORDS).insertMany(
            listOf(
                Document("_id", photoRecord).append("picture", oldAddress("users/u1/photo") + "?v=1700000000000"),
                Document("_id", "by-name").append("kyc", Document("documents", listOf(oldAddress("kyc/u1/front"), "unrelated"))),
                Document("_id", 42).append("note", "see ${oldAddress("organization_logos/a")} for the logo"),
                Document("_id", Document("compound", 1)).append("logo", oldAddress("organization_logos/a")),
                Document("_id", oldAddress("users/u4/photo")).append("what", "an address as an id"),
                Document("_id", untouched).append("picture", newAddress("users/u9/photo"))
            )
        )

        val started = moves.start(old.id, new.id, "Test")
        assertEquals(State.SWITCHING, started.state)
        assertTrue(row(new).primary, "new files go to the target at once")
        assertTrue(!row(old).primary)

        assertEquals(State.SWITCHING, moves.step(started.id).state, "nothing is read before every instance has switched")
        assertEquals(6, from.files.size)

        clock.now = started.readNotBefore.plusSeconds(1)
        val (done, _) = runToEnd(started)

        assertEquals(State.DONE, done.state, done.error)
        assertEquals(6, done.filesTotal)
        assertEquals(4, done.filesCopied)
        assertEquals(2, done.filesAlreadyThere)
        assertEquals(0, done.filesFailed)
        assertEquals("photo-1".length + "sealed-front".length + "logo".length + "photo-4".length.toLong(), done.bytesCopied)

        assertEquals("photo-1", to.text("users/u1/photo"))
        assertEquals("sealed-front", to.text("kyc/u1/front"))
        assertEquals("photo-4", to.text("users/u4/photo"), "an older file of another size is replaced")
        assertEquals("written since the move began", to.text("users/u3/photo"), "a newer file is kept")
        assertEquals(6, from.files.size, "nothing is deleted from the old store")

        assertEquals(new.id, row(old).readFromStoreId)
        assertEquals("https://cdn.example.com/users/u1/photo", record(photoRecord).getString("picture"))
        assertEquals(
            listOf(newAddress("kyc/u1/front"), "unrelated"),
            record("by-name").get("kyc", Document::class.java).getList("documents", String::class.java)
        )
        assertEquals(newAddress("organization_logos/a"), record(Document("compound", 1)).getString("logo"))
        assertEquals("see ${oldAddress("organization_logos/a")} for the logo", record(42).getString("note"))
        assertEquals("an address as an id", record(oldAddress("users/u4/photo")).getString("what"))
        assertEquals(newAddress("users/u9/photo"), record(untouched).getString("picture"))
        assertEquals(3, done.linksUpdated)
        assertEquals(2, done.linksLeft, "the mention inside text and the id")
        assertEquals(done.recordsTotal, done.recordsRead)
        assertTrue(done.recordsRead >= 6)
        assertTrue(done.summary().contains("keep Old listed"), done.summary())

        // What is left still opens, from the copy.
        val router = RoutingStorageService(stores)
        assertEquals("photo-4", router.download(oldAddress("users/u4/photo"))?.decodeToString())
        assertEquals(newAddress("users/u5/photo"), router.upload("users/u5/photo", null, "5".toByteArray()))
        assertEquals(
            HttpStatus.CONFLICT,
            assertFailsWith<ResponseStatusException> { storeService.remove(new.id) }.statusCode,
            "the store the old one reads from has to stay listed"
        )
    }

    @Test
    fun `a move goes a slice at a time, each slice carrying on where the last stopped`() {
        services(TestClock(start, tick = Duration.ofSeconds(1)))
        val old = store("Old", "slice-old-files")
        val new = store("New", "slice-new-files", Kind.GCS)
        val from = bucketOf(old)
        val names = (1..40).map { "users/u%02d/photo".format(it) }
        names.forEach { from.put(it, "photo $it") }

        val (done, steps) = runToEnd(moves.start(old.id, new.id, null))

        assertEquals(State.DONE, done.state, done.error)
        assertTrue(steps > 5, "took $steps steps")
        assertEquals(40, done.filesTotal)
        assertEquals(40, done.filesCopied)
        val to = bucketOf(new)
        names.forEach { assertEquals(1, to.writes[it]?.get(), "$it should be copied exactly once") }
    }

    @Test
    fun `a slice another holds is not run again, and a lease nobody renews runs out`() {
        services()
        val old = store("Old", "lease-old-files")
        val new = store("New", "lease-new-files")
        val from = bucketOf(old)
        from.put("users/u1/photo", "photo")
        var listings = 0
        from.onList = { listings++ }

        val move = moves.start(old.id, new.id, null)
        clock.now = move.readNotBefore.plusSeconds(1)
        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(move.id)),
            Update().set("leaseOwner", "another-tab").set("leaseUntil", clock.now.plus(StorageMoveService.LEASE)),
            StorageMove::class.java
        )

        assertEquals(State.SWITCHING, moves.step(move.id).state)
        assertEquals(0, listings, "the slice held elsewhere was not run here")

        clock.now = clock.now.plus(StorageMoveService.LEASE).plusSeconds(1)
        val (done, _) = runToEnd(moveRepository.findById(move.id).get())
        assertEquals(State.DONE, done.state, done.error)
        assertEquals(1, done.filesCopied)
        assertNull(done.leaseOwner)
    }

    @Test
    fun `a cancel stops a slice already running before it changes anything outside the move`() {
        services()
        val old = store("Old", "cancel-old-files")
        val new = store("New", "cancel-new-files")
        val from = bucketOf(old)
        from.put("users/u1/photo", "photo")
        val oldAddress = old.addresses().addressOf("users/u1/photo")
        mongoTemplate.getCollection(RECORDS).insertOne(Document("_id", 1).append("picture", oldAddress))

        val move = moves.start(old.id, new.id, null)
        clock.now = move.readNotBefore.plusSeconds(1)
        var cancelled = false
        from.onList = {
            if (!cancelled) {
                cancelled = true
                moves.cancel(move.id)
            }
        }

        val after = moves.step(move.id)

        assertEquals(State.CANCELLED, after.state)
        assertEquals(0, after.filesCopied, "the cancelled slice's progress was not saved over the cancel")
        assertNull(row(old).readFromStoreId)
        assertEquals(oldAddress, record(1).getString("picture"))
        assertTrue(row(new).primary, "new files keep going where the move sent them")
    }

    @Test
    fun `a copy that fails for any file goes no further, and moving again finishes it`() {
        services()
        val old = store("Old", "failing-old-files")
        val new = store("New", "failing-new-files")
        val from = bucketOf(old)
        from.put("kyc/u1/front", "front")
        from.put("users/u1/photo", "photo")
        from.unreadable += "kyc/u1/front"
        val oldAddress = old.addresses().addressOf("users/u1/photo")
        mongoTemplate.getCollection(RECORDS).insertOne(Document("_id", 1).append("picture", oldAddress))

        val first = moves.start(old.id, new.id, null)
        clock.now = first.readNotBefore.plusSeconds(1)
        val (failed, _) = runToEnd(first)

        assertEquals(State.FAILED, failed.state)
        assertEquals(1, failed.filesFailed)
        assertEquals(listOf("kyc/u1/front"), failed.failedNames)
        assertEquals(1, failed.filesCopied)
        assertTrue(failed.error!!.contains("1 of 2 files could not be copied"), failed.error)
        assertNull(row(old).readFromStoreId, "the old store still serves its own files")
        assertEquals(oldAddress, record(1).getString("picture"))

        from.unreadable.clear()
        val again = moves.start(old.id, new.id, null)
        clock.now = again.readNotBefore.plusSeconds(1)
        val (done, _) = runToEnd(again)

        assertEquals(State.DONE, done.state, done.error)
        assertEquals(1, done.filesCopied)
        assertEquals(1, done.filesAlreadyThere)
        assertEquals(new.addresses().addressOf("users/u1/photo"), record(1).getString("picture"))
    }

    @Test
    fun `one move at a time, and the stores hold still while it runs`() {
        services()
        val old = store("Old", "single-old-files")
        val new = store("New", "single-new-files")
        val third = store("Third", "single-third-files")
        moves.start(old.id, new.id, null)

        val conflicts = listOf(
            { moves.start(new.id, third.id, null) },
            { storeService.makePrimary(third.id) },
            { storeService.remove(third.id) }
        )
        conflicts.forEach { attempt ->
            assertEquals(HttpStatus.CONFLICT, assertFailsWith<ResponseStatusException> { attempt() }.statusCode)
        }
    }

    @Test
    fun `a record changed between being read and being written keeps the change`() {
        services()
        val collection = mongoTemplate.getCollection(RECORDS)
        collection.insertOne(Document("_id", 1).append("picture", "https://old.example/users/u1/photo"))
        val plan = rewriter.plan(
            record(1),
            rewrite = { it.replace("https://old.example/", "https://new.example/").takeIf { new -> new != it } },
            mentions = { false }
        )
        collection.updateOne(Document("_id", 1), Document("\$set", Document("picture", "https://elsewhere.example/p")))

        assertEquals(0, rewriter.apply(RECORDS, plan.changes))
        assertEquals("https://elsewhere.example/p", record(1).getString("picture"))
    }

    @Test
    fun `records are read a batch at a time across every kind of id`() {
        services()
        val ids = listOf<Any>(3, 1, 2L, 2.5, "b", "a", ObjectId(), ObjectId(), Document("k", 1), true, Instant.EPOCH.let(java.util.Date::from))
        mongoTemplate.getCollection(RECORDS).insertMany(ids.map { Document("_id", it).append("file", "old:users/photo") })

        var position = AddressRewriter.Position()
        var read = 0L
        var updated = 0L
        var batches = 0
        while (true) {
            val batch = rewriter.rewriteBatch(listOf(RECORDS), position, 2, { if (it == "old:users/photo") "new:users/photo" else null }, { false })
            if (batch.finished) break
            check(++batches < 100)
            position = batch.position
            read += batch.recordsRead
            updated += batch.linksUpdated
        }

        assertEquals(ids.size.toLong(), read)
        assertEquals(ids.size.toLong(), updated)
        assertEquals(0, mongoTemplate.getCollection(RECORDS).countDocuments(Document("file", "old:users/photo")))
    }

    @Test
    fun `a value under a key no update can name is counted, not written`() {
        services()
        val record = Document("_id", "x")
            .append("odd.key", "old:a")
            .append("\$odd", "old:b")
            .append("nested", Document("list", listOf(Document("photo", "old:c"))))
            .append("note", "see old:d")
        val plan = rewriter.plan(record, { if (it.startsWith("old:")) "new:" + it.removePrefix("old:") else null }, { it.contains("old:") })

        assertEquals(listOf("nested.list.0.photo" to "new:c"), plan.changes.map { it.path to it.new })
        assertEquals(3, plan.left)
    }

    private fun StorageMove.summary() = pitampoudel.komposeauth.storage.dto.summaryOf(this)

    companion object {
        const val RECORDS = "storage_move_test_records"
    }
}
