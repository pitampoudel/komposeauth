package pitampoudel.komposeauth.core.migration

import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.stereotype.Component
import java.security.MessageDigest

/**
 * Creates the indexes the `@Indexed` and `@CompoundIndex` annotations describe, once per database
 * rather than once per process.
 *
 * Spring Data can do this by itself — `spring.data.mongodb.auto-index-creation` — and this server
 * used to let it. What that setting actually does is issue a `createIndex` for every index on every
 * mapped type while the application context is refreshing, which is around thirty commands here.
 * Against a managed cluster a round trip is tens of milliseconds, so a cold start spends the better
 * part of a second re-asserting indexes that have existed since the first deployment, and on a
 * service that scales to zero somebody is waiting through it. Spring Data's own documentation
 * recommends against the setting in production for roughly this reason.
 *
 * The work still happens automatically and still comes from the annotations — [
 * MongoPersistentEntityIndexResolver] is the same resolver the automatic path uses, so the
 * definitions are identical. The only change is that a run is recorded in `schema_migrations` and
 * skipped next time.
 *
 * Which raises the obvious hazard: a migration that has already run never runs again, so adding an
 * `@Indexed` later would do nothing. Hence the fingerprint in [id]. It is derived from the resolved
 * definitions themselves, so any annotation change anywhere produces an id the collection has not
 * seen and the indexes are reconciled on the next deployment.
 */
@Component
class MongoIndexMigration(
    private val mappingContext: MongoMappingContext
) : MongoMigration {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Ahead of any data migration, which may well rely on an index to find the rows it rewrites. */
    override val order: Int = Int.MIN_VALUE

    /**
     * Computed once and held, because [MongoMigrationRunner] reads it twice — to reserve the run and
     * again to release the reservation if the run fails — and the two must agree.
     */
    override val id: String by lazy { "mongo-indexes-${fingerprint()}" }

    override fun migrate(mongoTemplate: MongoTemplate) {
        var created = 0
        forEachIndex { holder ->
            // Addressed by the collection the resolver names rather than by the owning entity's own.
            // For an index declared on a nested field the two are the same, but this is what
            // MongoPersistentEntityIndexCreator does, and the point of this class is to be that path
            // on a different schedule -- not a second opinion about where an index belongs.
            mongoTemplate.indexOps(holder.collection).createIndex(holder.indexDefinition)
            created++
        }
        log.info("Ensured {} Mongo indexes", created)
    }

    /**
     * A stable digest of every index this codebase declares.
     *
     * Sorted before hashing: the mapping context iterates entities in whatever order the classpath
     * scan produced, and an id that moved with it would make the migration re-run on some startups
     * and not others — the exact cost this class exists to avoid.
     */
    private fun fingerprint(): String {
        val descriptions = sortedSetOf<String>()
        forEachIndex { holder ->
            descriptions.add(
                "${holder.collection}|${holder.indexKeys.toJson()}|${holder.indexOptions.toJson()}"
            )
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(descriptions.joinToString("\n").toByteArray())
        return digest.take(6).joinToString("") { "%02x".format(it) }
    }

    private inline fun forEachIndex(
        action: (holder: MongoPersistentEntityIndexResolver.IndexDefinitionHolder) -> Unit
    ) {
        val resolver = MongoPersistentEntityIndexResolver(mappingContext)
        mappingContext.persistentEntities
            .filter { it.isAnnotationPresent(Document::class.java) }
            .forEach { entity ->
                resolver.resolveIndexFor(entity.typeInformation).forEach { holder ->
                    action(holder)
                }
            }
    }
}
