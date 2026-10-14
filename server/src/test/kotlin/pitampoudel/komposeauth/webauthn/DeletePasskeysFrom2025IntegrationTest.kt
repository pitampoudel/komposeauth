package pitampoudel.komposeauth.webauthn

import com.mongodb.client.model.Filters
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.security.web.webauthn.api.AuthenticatorTransport
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.webauthn.entity.PublicKeyCredential
import pitampoudel.komposeauth.webauthn.entity.PublicKeyUser
import pitampoudel.komposeauth.webauthn.migration._0001__DeletePasskeysFrom2025Change
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * A passkey registered before January 2026 is deleted, so its owner can register a new one, and a
 * current one is kept. One old user only: the unique indexes on the fields old users lack refuse a
 * second, as they have on every start since January.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class DeletePasskeysFrom2025IntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var userEntities: PublicKeyCredentialUserEntityRepository

    @Autowired
    private lateinit var credentialRecords: UserCredentialRepository

    private val users get() = mongoTemplate.getCollection("public_key_users")
    private val credentials get() = mongoTemplate.getCollection("public_key_credentials")

    private val accounts = mutableListOf<ObjectId>()
    private val oldUsers = mutableListOf<String>()
    private val credentialIds = mutableListOf<Bytes>()

    private fun account(): ObjectId {
        val id = ObjectId()
        mongoTemplate.insert(User(id = id, firstName = "A", lastName = "B", email = "p$id@example.com", phoneNumber = null))
        accounts += id
        return id
    }

    /** As the releases before January 2026 stored one: keyed by its handle as text, naming no account. */
    private fun oldUser(handle: Bytes, name: String) {
        val id = handle.toBase64UrlString()
        users.insertOne(
            Document("_id", id).append("name", name).append("displayName", "A B")
                .append("createdAt", Date()).append("updatedAt", Date()).append("_type", "public_key_user")
        )
        oldUsers += id
    }

    /** A passkey as stored now, or, with [old], as before January 2026: `lastUsed` and no account. */
    private fun credential(handle: Bytes, account: ObjectId, old: Boolean): Bytes {
        val credential = PublicKeyCredential(
            id = Bytes.random(), publicKeyUserId = handle, userId = account, label = "Phone",
            signatureCount = 3, credentialType = PublicKeyCredentialType.PUBLIC_KEY,
            publicKey = ImmutablePublicKeyCose(byteArrayOf(1, 2, 3)), attestationClientDataJSON = null,
            attestationObject = null, transports = setOf(AuthenticatorTransport.INTERNAL), backupEligible = true,
            backupState = true, uvInitialized = true, lastUsedAt = Instant.parse("2025-11-02T09:30:00Z")
        )
        val stored = Document().also { mongoTemplate.converter.write(credential, it) }
        if (old) stored.apply { remove("userId"); put("lastUsed", remove("lastUsedAt")) }
        credentials.insertOne(stored)
        credentialIds += credential.id
        return credential.id
    }

    @AfterEach
    fun tearDown() {
        users.deleteMany(Filters.or(Filters.`in`("userId", accounts), Filters.`in`("_id", oldUsers)))
        credentialIds.forEach { credentials.deleteOne(Filters.eq("_id", mongoTemplate.converter.convertToMongoType(it))) }
        mongoTemplate.getCollection("users").deleteMany(Filters.`in`("_id", accounts))
    }

    @Test
    fun `a passkey from 2025 is deleted and its owner can register again`() {
        val owner = account()
        val oldHandle = Bytes.random()
        oldUser(oldHandle, owner.toHexString())
        val oldCredential = credential(oldHandle, owner, old = true)
        assertFails("the old user can't be read") { userEntities.findByUsername(owner.toHexString()) }

        val current = account()
        val currentHandle = Bytes.random()
        mongoTemplate.insert(
            PublicKeyUser(id = ObjectId(), userId = current, userHandle = currentHandle, name = current.toHexString(), displayName = "A B")
        )
        val currentCredential = credential(currentHandle, current, old = false)

        val change = _0001__DeletePasskeysFrom2025Change()
        change.apply(mongoTemplate)
        change.apply(mongoTemplate)

        assertNull(users.find(Filters.eq("_id", oldHandle.toBase64UrlString())).first())
        assertNull(credentialRecords.findByCredentialId(oldCredential))
        assertNotNull(userEntities.findByUsername(owner.toHexString()), "a new passkey user is made for the owner")
        assertEquals(currentHandle, userEntities.findById(currentHandle)?.id)
        assertNotNull(credentialRecords.findByCredentialId(currentCredential))
    }
}
