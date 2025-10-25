package com.vardansoft.komposeauth.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class RegisterPublicKeyRequest(val publicKey: PublicKeyCredential){
    @Serializable
    data class PublicKeyCredential(val credential: JsonObject, val label: String)
}

