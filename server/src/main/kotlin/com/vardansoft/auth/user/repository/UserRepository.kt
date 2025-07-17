package com.vardansoft.auth.user.repository

import com.vardansoft.auth.user.entity.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : MongoRepository<User, ObjectId> {
    fun findByEmail(email: String): User?
}