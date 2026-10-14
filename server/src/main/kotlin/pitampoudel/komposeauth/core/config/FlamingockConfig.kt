package pitampoudel.komposeauth.core.config

import com.mongodb.client.MongoClient
import io.flamingock.api.annotations.EnableFlamingock
import io.flamingock.api.annotations.Stage
import io.flamingock.internal.core.external.store.CommunityAuditStore
import io.flamingock.store.mongodb.sync.MongoDBSyncAuditStore
import io.flamingock.targetsystem.mongodb.sync.MongoDBSyncTargetSystem
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate

/**
 * Wires Flamingock, which runs every change to stored data: once per database, in order, recorded in
 * its own `flamingockAuditLog` collection. `flamingock-springboot-integration` builds and runs the
 * `ApplicationRunner` from the two beans below and the stages named here.
 *
 * A change that throws stops the server starting, and the next start runs it again. One cut off
 * midway, by an instance stopped while it starts, runs again only because it says so with
 * `@Recovery(strategy = ALWAYS_RETRY)`: without that, Flamingock refuses every later start until
 * somebody resolves it by hand. So each change is written to be safe to run twice, and says so.
 *
 * A change is a class in the `migration` package of the feature whose data it rewrites, named
 * `_NNNN__Description` (the number is its order within the stage, and the stages run in the order
 * listed here), with `@Change` and an `@Apply` method taking the `MongoTemplate`. It works on stored
 * documents rather than through a service, whose rules are the current release's and not the data's.
 * A feature's first change also adds its [Stage] here, and `FlamingockStagesTest` fails until it
 * does, because a stage nobody declares never runs.
 *
 * Indexes are not changes: Spring Data creates the ones the entities declare at every start
 * (`spring.data.mongodb.auto-index-creation`), before Flamingock runs, so a change can rely on them.
 */
@Configuration
@EnableFlamingock(
    stages = [
        Stage(name = "storage", location = "pitampoudel.komposeauth.storage.migration"),
        Stage(name = "user", location = "pitampoudel.komposeauth.user.migration"),
        Stage(name = "authorization", location = "pitampoudel.komposeauth.authorization.migration"),
        Stage(name = "oauth_clients", location = "pitampoudel.komposeauth.oauth_clients.migration"),
        Stage(name = "kyc", location = "pitampoudel.komposeauth.kyc.migration"),
        Stage(name = "organization", location = "pitampoudel.komposeauth.organization.migration"),
        Stage(name = "webauthn", location = "pitampoudel.komposeauth.webauthn.migration"),
        Stage(name = "core", location = "pitampoudel.komposeauth.core.migration")
    ]
)
class FlamingockConfig {

    @Bean
    fun mongoDBSyncTargetSystem(mongoClient: MongoClient, mongoTemplate: MongoTemplate): MongoDBSyncTargetSystem =
        MongoDBSyncTargetSystem(TARGET_SYSTEM, mongoClient, mongoTemplate.db.name)

    @Bean
    fun communityAuditStore(mongoDBSyncTargetSystem: MongoDBSyncTargetSystem): CommunityAuditStore =
        MongoDBSyncAuditStore.from(mongoDBSyncTargetSystem)

    companion object {
        /** What every change names in its `@TargetSystem`. */
        const val TARGET_SYSTEM = "mongodb-sync"
    }
}
