package pitampoudel.komposeauth.user.repository

import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import pitampoudel.komposeauth.user.entity.User
import kotlin.jvm.optionals.getOrNull
import java.util.regex.Pattern

@Repository
interface UserRepository : MongoRepository<User, ObjectId>, UserRepositoryCustom {
    fun findByEmail(email: String): User?
    fun findByPhoneNumber(phoneNumber: String): User?
    fun findByIdIn(ids: List<ObjectId>): List<User>
    fun findByRolesContaining(role: String, pageable: Pageable): Page<User>
    fun countByRolesContaining(role: String): Long

    fun findByUserName(value: String): User? {
        var user: User? = null
        if (ObjectId.isValid(value)) {
            user = findById(ObjectId(value)).getOrNull()
        }
        user = user ?: findByEmail(value)
        user = user ?: findByPhoneNumber(value)
        return user
    }
}

interface UserRepositoryCustom {
    fun search(tokens: List<String>, pageable: Pageable): Page<User>
}

class UserRepositoryImpl(
    private val mongoTemplate: MongoTemplate
) : UserRepositoryCustom {
    override fun search(tokens: List<String>, pageable: Pageable): Page<User> {
        if (tokens.isEmpty()) return Page.empty(pageable)

        val criteriaPerToken = tokens.map { token ->
            val regex = Pattern.compile(".*${Pattern.quote(token)}.*", Pattern.CASE_INSENSITIVE)
            Criteria().orOperator(
                Criteria.where("firstName").regex(regex),
                Criteria.where("lastName").regex(regex),
                Criteria.where("email").regex(regex),
                Criteria.where("phoneNumber").regex(regex)
            )
        }

        val query = Query().addCriteria(Criteria().andOperator(*criteriaPerToken.toTypedArray()))
        val total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), User::class.java)
        val results = mongoTemplate.find(query.with(pageable), User::class.java)

        return PageImpl(results, pageable, total)
    }
}
