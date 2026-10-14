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
import pitampoudel.komposeauth.user.migration._0002__StorePhoneNumbersInE164Change
import pitampoudel.komposeauth.user.repository.UserRepository
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A number the releases before December 2025 stored in another format is found again by its E.164 lookup. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class StorePhoneNumbersInE164IntegrationTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var userRepository: UserRepository

    private val created = mutableListOf<ObjectId>()

    /** Seven random digits, so the numbers of other tests' accounts are never the ones compared. */
    private val line = Random.nextInt(1_000_000, 9_999_999).toString()

    private fun account(phone: String): ObjectId {
        val user = User(id = ObjectId(), firstName = "A", lastName = "B", email = null, phoneNumber = phone)
        mongoTemplate.insert(user)
        created += user.id
        return user.id
    }

    private fun phoneOf(id: ObjectId) = mongoTemplate.findById(id, User::class.java)!!.phoneNumber

    @AfterEach
    fun tearDown() {
        mongoTemplate.remove(Query(Criteria.where("_id").`in`(created)), User::class.java)
    }

    @Test
    fun `numbers stored in other formats are stored in E164 and found again`() {
        val international = account("+977 984-$line")
        val local = account("985$line")
        val e164 = account("+977986$line")
        assertNull(userRepository.findByLoginName("984$line", "NP"), "the lookup asks for E.164 and the stored number is not")

        _0002__StorePhoneNumbersInE164Change().apply(mongoTemplate)

        assertEquals("+977984$line", phoneOf(international))
        assertEquals("+977985$line", phoneOf(local))
        assertEquals("+977986$line", phoneOf(e164))
        assertEquals(international, userRepository.findByLoginName("984$line", "NP")?.id)
    }

    @Test
    fun `a number another account holds, or that is no number, is left as it is`() {
        val holder = account("+977984$line")
        val other = account("+977 984-$line")
        val first = account("+977 985-$line")
        val second = account("985$line")
        val unreadable = account("$line$line$line")

        val change = _0002__StorePhoneNumbersInE164Change()
        change.apply(mongoTemplate)
        change.apply(mongoTemplate)

        assertEquals("+977984$line", phoneOf(holder))
        assertEquals("+977 984-$line", phoneOf(other))
        assertEquals("+977985$line", phoneOf(first))
        assertEquals("985$line", phoneOf(second))
        assertEquals("$line$line$line", phoneOf(unreadable), "too long to be a phone number anywhere")
    }
}
