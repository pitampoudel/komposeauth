package com.vardansoft.komposeauth.user.repository

import com.vardansoft.komposeauth.user.entity.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : MongoRepository<User, ObjectId> {
    fun findByEmail(email: String): User?
    fun findByPhoneNumber(phoneNumber: String): User?
    fun findByIdIn(ids: List<ObjectId>): List<User>
}
