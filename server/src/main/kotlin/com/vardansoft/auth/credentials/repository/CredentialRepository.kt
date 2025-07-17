package com.vardansoft.auth.credentials.repository

import com.vardansoft.auth.credentials.entity.Credential
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface CredentialRepository : MongoRepository<Credential, ObjectId> {
    fun findByUserId(userId: ObjectId): List<Credential>
}