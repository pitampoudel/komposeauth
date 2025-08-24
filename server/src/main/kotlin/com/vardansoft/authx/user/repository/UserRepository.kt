package com.vardansoft.authx.user.repository

import com.vardansoft.authx.user.entity.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : MongoRepository<User, ObjectId> {
    fun findByEmail(email: String): User?
    fun findByIdIn(ids: List<ObjectId>): List<User>
}
