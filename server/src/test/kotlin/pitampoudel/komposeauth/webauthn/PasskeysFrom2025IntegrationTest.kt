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
import pitampoudel.komposeauth.webauthn.migration._0001__PasskeysFrom2025Change
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * A passkey registered before January 2026 signs its owner in again, read through the two
 * repositories sign-in reads it through. Each test stores one old user only: the unique indexes on
 * the fields old users lack would refuse a second, as they have on every start since January.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class PasskeysFrom2025IntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var userEntities: PublicKeyCredentialUserEntityRepository

    @Autowired
    private lateinit var credentialRecords: UserCredentialRepository

    private val users get() = mongoTemplate.getCollection("public_key_users")
    private val usersLegacy get() = mongoTemplate.getCollection("public_key_users_legacy")
    private val credentials get() = mongoTemplate.getCollection("public_key_credentials")
    private val credentialsLegacy get() = mongoTemplate.getCollection("public_key_credentials_legacy")

    private val accounts = mutableListOf<ObjectId>()
    private val oldUsers = mutableListOf<String>()
    private val credentialIds = mutableListOf<Bytes>()

    private val lastUsed = Instant.parse("2025-11-02T09:30:00Z")

    private fun account(): User {
        val id = ObjectId()
        val user = User(id = id, firstName = "A", lastName = "B", email = "p$id@example.com", phoneNumber = null)
        mongoTemplate.insert(user)
        accounts += id
        return user
    }

    /** As the releases before January 2026 stored one: keyed by its handle as text, naming no account. */
    private fun oldUser(handle: Bytes, name: String, nameField: String = "name") {
        val id = handle.toBase64UrlString()
        users.insertOne(
            Document("_id", id).append(nameField, name).append("displayName", "A B")
                .append("createdAt", Date()).append("updatedAt", Date()).append("_type", "public_key_user")
        )
        oldUsers += id
    }

    /** As they stored a passkey: `lastUsed`, and no account. */
    private fun oldCredential(handle: Bytes): Bytes {
        val credential = PublicKeyCredential(
            id = Bytes.random(), publicKeyUserId = handle, userId = ObjectId(), label = "Phone",
            signatureCount = 3, credentialType = PublicKeyCredentialType.PUBLIC_KEY,
            publicKey = ImmutablePublicKeyCose(byteArrayOf(1, 2, 3)), attestationClientDataJSON = null,
            attestationObject = null, transports = setOf(AuthenticatorTransport.INTERNAL), backupEligible = true,
            backupState = true, uvInitialized = true, lastUsedAt = lastUsed
        )
        val stored = Document().also { mongoTemplate.converter.write(credential, it) }
            .apply { remove("userId"); put("lastUsed", remove("lastUsedAt")) }
        credentials.insertOne(stored)
        credentialIds += credential.id
        return credential.id
    }

    private fun migrate() {
        val change = _0001__PasskeysFrom2025Change()
        change.apply(mongoTemplate)
        change.apply(mongoTemplate)
    }

    private fun stored(id: Bytes) = mongoTemplate.converter.convertToMongoType(id)

    @AfterEach
    fun tearDown() {
        users.deleteMany(Filters.or(Filters.`in`("userId", accounts), Filters.`in`("_id", oldUsers)))
        usersLegacy.deleteMany(Filters.`in`("_id", oldUsers))
        credentialIds.map(::stored).forEach {
            credentials.deleteOne(Filters.eq("_id", it))
            credentialsLegacy.deleteOne(Filters.eq("_id", it))
        }
        mongoTemplate.getCollection("users").deleteMany(Filters.`in`("_id", accounts))
    }

    @Test
    fun `a passkey from 2025 finds its account again`() {
        val account = account()
        val handle = Bytes.random()
        oldUser(handle, account.id.toHexString())
        val credentialId = oldCredential(handle)
        assertFails("the old user can't be read") { userEntities.findByUsername(account.id.toHexString()) }

        migrate()

        assertEquals(account.id.toHexString(), userEntities.findById(handle)?.name)
        assertEquals(handle, userEntities.findByUsername(account.id.toHexString())?.id)
        val credential = credentialRecords.findByCredentialId(credentialId) as PublicKeyCredential
        assertEquals(account.id, credential.userId)
        assertEquals(lastUsed, credential.lastUsedAt)
        assertEquals(listOf(credentialId), credentialRecords.findByUserId(handle).map { it.credentialId })
    }

    @Test
    fun `one the first releases named by email finds its account too`() {
        val account = account()
        val handle = Bytes.random()
        oldUser(handle, account.email!!.uppercase(), nameField = "email")
        val credentialId = oldCredential(handle)

        migrate()

        assertEquals(account.email!!.uppercase(), userEntities.findById(handle)?.name)
        assertEquals(account.id, (credentialRecords.findByCredentialId(credentialId) as PublicKeyCredential).userId)
    }

    @Test
    fun `one whose account has registered a passkey since is set aside`() {
        val account = account()
        val current = Bytes.random()
        mongoTemplate.insert(
            PublicKeyUser(
                id = ObjectId(), userId = account.id, userHandle = current,
                name = account.id.toHexString(), displayName = "A B"
            )
        )
        val handle = Bytes.random()
        oldUser(handle, account.email!!)
        val credentialId = oldCredential(handle)

        migrate()

        assertEquals(current, userEntities.findByUsername(account.id.toHexString())?.id)
        assertNull(userEntities.findById(handle))
        assertNotNull(usersLegacy.find(Filters.eq("_id", handle.toBase64UrlString())).first())
        assertNull(credentialRecords.findByCredentialId(credentialId))
        assertNotNull(credentialsLegacy.find(Filters.eq("_id", stored(credentialId))).first())
    }

    @Test
    fun `one whose name finds no account is set aside`() {
        val handle = Bytes.random()
        oldUser(handle, "gone-${ObjectId()}@example.com")
        val credentialId = oldCredential(handle)

        migrate()

        assertNull(userEntities.findById(handle))
        assertNotNull(usersLegacy.find(Filters.eq("_id", handle.toBase64UrlString())).first())
        assertNull(credentialRecords.findByCredentialId(credentialId))
        assertNotNull(credentialsLegacy.find(Filters.eq("_id", stored(credentialId))).first())
    }
}
