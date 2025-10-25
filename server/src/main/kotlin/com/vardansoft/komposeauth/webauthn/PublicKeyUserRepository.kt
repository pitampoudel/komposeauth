package com.vardansoft.komposeauth.webauthn

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PublicKeyUserRepository : MongoRepository<PublicKeyUser, String> {
    fun findByName(name: String): PublicKeyUser?
}
