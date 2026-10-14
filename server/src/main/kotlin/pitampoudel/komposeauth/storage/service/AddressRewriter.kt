package pitampoudel.komposeauth.storage.service

import com.mongodb.MongoBulkWriteException
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.Updates
import org.bson.Document
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

/**
 * Finds every saved address of a moved file, in every collection, and points it at the new store.
 *
 * Nothing here knows which fields hold a file: a profile photo, a logo and a KYC document are three
 * different shapes in three collections today, and a fourth would be missed by a list of them. So
 * every string of every record is looked at, and one is rewritten only when the whole string is one
 * address of the old store. A string that merely contains one (inside some longer text) is counted
 * and left alone, since there is no telling what else that text means.
 *
 * Each value is written back with its old value in the filter, so a record changed while this ran
 * keeps the change and is counted as left rather than overwritten.
 */
@Component
class AddressRewriter(private val mongoTemplate: MongoTemplate) {

    /** Where the walk is: which collection, which kind of `_id`, and the last `_id` read in it. */
    data class Position(val collectionIndex: Int = 0, val idBracket: Int = 0, val lastIdJson: String? = null)

    data class Batch(
        val position: Position,
        val recordsRead: Long,
        val linksUpdated: Long,
        val linksLeft: Long,
        val finished: Boolean
    )

    /** One value to rewrite: the dotted [path] in the record with `_id` [id], from [old] to [new]. */
    data class Change(val id: Any?, val path: String, val old: String, val new: String)

    /** What reading one record found: the values to rewrite and how many it can't. */
    data class Plan(val changes: List<Change>, val left: Long)

    /** Every collection a file's address might be saved in, fixed when the rewrite begins. */
    fun collections(): List<String> =
        mongoTemplate.db.listCollections()
            .filter(Filters.eq("type", "collection"))
            .map { it.getString("name") }
            .filter { name -> !name.startsWith("system.") && name !in SKIPPED }
            .toList()
            .sorted()

    fun recordsIn(collections: List<String>): Long =
        collections.sumOf { mongoTemplate.getCollection(it).estimatedDocumentCount() }

    /**
     * Reads up to [limit] records from [position] on, rewrites what [rewrite] answers for, and says
     * where to carry on. [rewrite] answers the new address for a whole old one, or null; [mentions]
     * says whether a string mentions an old address anywhere.
     */
    fun rewriteBatch(
        collections: List<String>,
        position: Position,
        limit: Int,
        rewrite: (String) -> String?,
        mentions: (String) -> Boolean
    ): Batch {
        var at = position
        while (at.collectionIndex < collections.size && at.idBracket >= ID_BRACKETS.size) {
            at = Position(at.collectionIndex + 1)
        }
        if (at.collectionIndex >= collections.size) return Batch(at, 0, 0, 0, finished = true)

        val collection = mongoTemplate.getCollection(collections[at.collectionIndex])
        val idCondition = Document("\$type", ID_BRACKETS[at.idBracket])
        at.lastIdJson?.let { idCondition.append("\$gt", idFromJson(it)) }
        val filter = Document("_id", idCondition)
        val records = collection.find(filter).sort(Sorts.ascending("_id")).limit(limit).toList()

        var left = 0L
        val changes = mutableListOf<Change>()
        records.forEach { record ->
            val plan = plan(record, rewrite, mentions)
            changes += plan.changes
            left += plan.left
        }
        val updated = apply(collections[at.collectionIndex], changes)
        left += changes.size - updated

        val next = if (records.size < limit) {
            Position(at.collectionIndex, at.idBracket + 1)
        } else {
            at.copy(lastIdJson = idToJson(records.last()["_id"]))
        }
        return Batch(next, records.size.toLong(), updated, left, finished = false)
    }

    /** The values in [record] to rewrite, and how many mention an old address but can't be. */
    fun plan(record: Document, rewrite: (String) -> String?, mentions: (String) -> Boolean): Plan {
        val id = record["_id"]
        val changes = mutableListOf<Change>()
        var left = 0L

        fun visit(value: Any?, path: String?) {
            when (value) {
                is String -> {
                    val new = rewrite(value)?.takeIf { it != value }
                    when {
                        new != null && path != null -> changes += Change(id, path, value, new)
                        new != null || mentions(value) -> left++
                    }
                }

                is Map<*, *> -> value.forEach { (key, child) ->
                    val name = key as? String
                    // A path can't name a key holding a dot or starting with a dollar, so its value is
                    // still read for addresses, only never written.
                    val nameable = path != null && name != null && name.isNotEmpty() && '.' !in name && !name.startsWith('$')
                    visit(child, if (nameable) (if (path!!.isEmpty()) name else "$path.$name") else null)
                }

                is List<*> -> value.forEachIndexed { index, child -> visit(child, path?.let { "$it.$index" }) }
            }
        }

        record.forEach { (key, value) ->
            // `_id` is the one value an update can't change.
            val nameable = key != "_id" && key.isNotEmpty() && '.' !in key && !key.startsWith('$')
            visit(value, if (nameable) key else null)
        }
        return Plan(changes, left)
    }

    /** Writes [changes], each only where the value is still the old one, and answers how many were. */
    fun apply(collection: String, changes: List<Change>): Long {
        if (changes.isEmpty()) return 0
        val writes = changes.map { change ->
            UpdateOneModel<Document>(
                Filters.and(Filters.eq("_id", change.id), Filters.eq(change.path, change.old)),
                Updates.set(change.path, change.new)
            )
        }
        return try {
            mongoTemplate.getCollection(collection).bulkWrite(writes, BulkWriteOptions().ordered(false)).matchedCount.toLong()
        } catch (ex: MongoBulkWriteException) {
            // A write Mongo refuses (a path that turned out to cross a value of another type) is left
            // and counted, rather than failing every slice that reaches this record again.
            ex.writeResult.matchedCount.toLong()
        }
    }

    companion object {
        /** Holds the stores' own settings and the move's progress, neither of which is a file's address. */
        private val SKIPPED = setOf("storage_stores", "storage_moves", "schema_migrations")

        /**
         * The kinds of `_id` in the order Mongo sorts them. A `$gt` only compares values of one kind,
         * so a collection whose ids are mixed is read one kind at a time, and each kind in order.
         */
        val ID_BRACKETS: List<List<String>> = listOf(
            listOf("minKey"),
            listOf("null"),
            listOf("double", "int", "long", "decimal"),
            listOf("symbol", "string"),
            listOf("object"),
            listOf("binData"),
            listOf("objectId"),
            listOf("bool"),
            listOf("date"),
            listOf("timestamp"),
            listOf("regex"),
            listOf("maxKey")
        )

        private val EXTENDED_JSON: JsonWriterSettings = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build()

        /** An `_id` as extended JSON, which keeps its exact type, so the next slice compares it the same. */
        fun idToJson(id: Any?): String = Document("v", id).toJson(EXTENDED_JSON)

        fun idFromJson(json: String): Any? = Document.parse(json)["v"]
    }
}
