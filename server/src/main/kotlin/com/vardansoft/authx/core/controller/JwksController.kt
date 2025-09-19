package com.vardansoft.authx.core.controller

import com.nimbusds.jose.jwk.JWKSet
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class JwksController(
    private val jwkSet: JWKSet
) {
    @GetMapping("/oauth2/jwks", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oauth2Jwks(): Map<String, Any> {
        return jwkSet.toJSONObject()
    }
}
