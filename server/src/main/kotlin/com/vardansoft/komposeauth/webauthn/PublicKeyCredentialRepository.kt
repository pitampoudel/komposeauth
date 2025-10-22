package com.vardansoft.komposeauth.webauthn

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.stereotype.Repository

@Repository
interface PublicKeyCredentialRepository : MongoRepository<PublicKeyCredential, Bytes> {
    fun findAllByPublicKeyUserId(publicKeyUserId: Bytes): List<PublicKeyCredential>
}