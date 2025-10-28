package pitampoudel.komposeauth.user.repository

import pitampoudel.komposeauth.user.entity.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import kotlin.jvm.optionals.getOrNull

@Repository
interface UserRepository : MongoRepository<User, ObjectId> {
    fun findByEmail(email: String): User?
    fun findByPhoneNumber(phoneNumber: String): User?
    fun findByIdIn(ids: List<ObjectId>): List<User>

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
