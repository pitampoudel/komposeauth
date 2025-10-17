package com.vardansoft.komposeauth.core.controller

import com.nimbusds.jose.jwk.JWKSet
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class JwksController(
    private val jwkSet: JWKSet
) {
    @Operation(
        summary = "Get JWKS",
        description = "Returns the JSON Web Key Set (JWKS) for token verification."
    )
    @GetMapping("/oauth2/jwks", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun jwks(): Map<String, Any> {
        return jwkSet.toJSONObject()
    }
}
