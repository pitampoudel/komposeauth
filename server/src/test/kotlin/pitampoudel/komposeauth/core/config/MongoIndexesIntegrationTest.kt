package pitampoudel.komposeauth.core.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.authorization.OAuth2AuthorizationDocument
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.webauthn.entity.PublicKeyUser
import kotlin.test.assertTrue

/**
 * The indexes the `@Indexed` and `@CompoundIndex` annotations declare exist, created by Spring Data
 * at startup (`spring.data.mongodb.auto-index-creation`).
 *
 * A missing index fails nothing: queries just get slower, a TTL collection stops pruning itself and
 * a uniqueness constraint that was relied on stops holding. So assert the indexes are there.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class MongoIndexesIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var mappingContext: MongoMappingContext

    @Test
    fun `spring data creates the declared indexes at startup`() {
        // Boot 4 moved the connection properties to `spring.mongodb.*` and left this one under
        // `spring.data.mongodb.*`, so a setting under the wrong prefix would quietly create nothing.
        assertTrue(mappingContext.isAutoIndexCreation, "spring.data.mongodb.auto-index-creation is off")
    }

    private fun indexedFieldsOf(type: Class<*>): Set<String> =
        mongoTemplate.indexOps(type).indexInfo
            .flatMap { info -> info.indexFields.map { it.key } }
            .toSet()

    @Test
    fun `unique indexes declared by annotations exist on the collection`() {
        // PublicKeyUser carries three separate @Indexed(unique = true) fields; User's uniqueness
        // comes from @CompoundIndexes. Between them they cover both resolution paths.
        val credentialUserIndexes = indexedFieldsOf(PublicKeyUser::class.java)
        assertTrue(
            credentialUserIndexes.size > 1,
            "expected the annotated indexes on PublicKeyUser, found $credentialUserIndexes"
        )

        val userIndexes = indexedFieldsOf(User::class.java)
        assertTrue(
            userIndexes.size > 1,
            "expected the compound indexes on User, found $userIndexes"
        )
    }

    @Test
    fun `an account's authorizations are found by an index when it is signed out everywhere`() {
        assertTrue(
            "principalName" in indexedFieldsOf(OAuth2AuthorizationDocument::class.java),
            "oauth2_authorizations has no index on principalName, so revoking an account scans the collection"
        )
    }

    @Test
    fun `ttl indexes exist, so expiring collections still prune themselves`() {
        // These are the ones with no other symptom when missing: rate-limit windows and OAuth
        // authorizations would simply accumulate forever.
        val rateLimitTtl = mongoTemplate.indexOps("rate_limits").indexInfo
            .any { it.expireAfter.isPresent }
        assertTrue(rateLimitTtl, "rate_limits lost its TTL index")

        // Never read through a repository, so only the entity scan creates it.
        val passkeyChallengeTtl = mongoTemplate.indexOps("webauthn_challenges").indexInfo
            .any { it.expireAfter.isPresent }
        assertTrue(passkeyChallengeTtl, "webauthn_challenges lost its TTL index")
    }
}
