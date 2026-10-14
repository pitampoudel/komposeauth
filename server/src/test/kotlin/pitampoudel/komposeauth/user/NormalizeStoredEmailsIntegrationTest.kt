package pitampoudel.komposeauth.user

import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.ActiveProfiles
import pitampoudel.komposeauth.TestConfig
import pitampoudel.komposeauth.user.entity.User
import pitampoudel.komposeauth.user.migration._0001__NormalizeStoredEmailsChange
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** An address the release before stored as it was typed is found again by the lookup that normalizes it. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class NormalizeStoredEmailsIntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var userRepository: UserRepository

    private val created = mutableListOf<ObjectId>()

    /** Unique to this run, so the addresses of other tests' accounts are never the ones compared. */
    private val domain = "n${ObjectId.get().toHexString()}.example"

    private fun account(email: String): ObjectId {
        val user = User(id = ObjectId(), firstName = "A", lastName = "B", email = email, phoneNumber = null)
        mongoTemplate.insert(user)
        created += user.id
        return user.id
    }

    private fun emailOf(id: ObjectId) = mongoTemplate.findById(id, User::class.java)!!.email

    @AfterEach
    fun tearDown() {
        mongoTemplate.remove(Query(Criteria.where("_id").`in`(created)), User::class.java)
    }

    @Test
    fun `addresses stored as typed are normalized and found again`() {
        val typed = account(" Alice@${domain.uppercase()} ")
        val normal = account("bob@$domain")
        assertNull(userRepository.findByUserName("Alice@$domain"), "the lookup normalizes and the stored address is not")

        _0001__NormalizeStoredEmailsChange().apply(mongoTemplate)

        assertEquals("alice@$domain", emailOf(typed))
        assertEquals("bob@$domain", emailOf(normal))
        assertEquals(typed, userRepository.findByUserName("Alice@$domain")?.id)
    }

    @Test
    fun `an address another account already holds is left as it is`() {
        val holder = account("carol@$domain")
        val other = account("Carol@$domain")
        val first = account("Dave@$domain")
        val second = account("DAVE@$domain")

        val change = _0001__NormalizeStoredEmailsChange()
        change.apply(mongoTemplate)
        change.apply(mongoTemplate)

        assertEquals("carol@$domain", emailOf(holder))
        assertEquals("Carol@$domain", emailOf(other))
        assertEquals("dave@$domain", emailOf(first))
        assertEquals("DAVE@$domain", emailOf(second))
    }
}
