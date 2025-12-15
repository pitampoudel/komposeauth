package pitampoudel.komposeauth.user.repository

import pitampoudel.komposeauth.user.entity.User
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import pitampoudel.core.data.parsePhoneNumber
import kotlin.jvm.optionals.getOrNull

@Repository
interface UserRepository : MongoRepository<User, ObjectId> {
    @Query($$"{ phoneNumber: { $exists: true }}")
    fun findAllHavingPhoneNumber(): List<User>
    fun findByEmail(email: String): User?
    fun findByPhoneNumber(phoneNumber: String): User?
    fun findByIdIn(ids: List<ObjectId>): List<User>
    fun findByRolesContaining(role: String, pageable: Pageable): Page<User>
    fun countByRolesContaining(role: String): Long

    // Search across common fields with pagination support
    fun findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneNumberContainingIgnoreCase(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: String,
        pageable: Pageable
    ): Page<User>

    fun findByUserName(value: String): User? {
        var user: User? = null
        if (ObjectId.isValid(value)) {
            user = findById(ObjectId(value)).getOrNull()
        }
        user = user ?: findByEmail(value)
        user = user ?: parsePhoneNumber(
            countryNameCode = null,
            phoneNumber = value
        )?.fullNumberInE164Format?.let {
            findByPhoneNumber(it)
        }
        return user
    }
}
