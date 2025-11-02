package pitampoudel.komposeauth.general.controller

import com.nimbusds.jose.jwk.JWKSet
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pitampoudel.komposeauth.AppProperties
import pitampoudel.komposeauth.data.ApiEndpoints.JWKS

@RestController
@RequestMapping
class ConfigController(
    private val jwkSet: JWKSet,
    private val appProperties: AppProperties
) {
    @Operation(
        summary = "Get JWKS",
        description = "Returns the JSON Web Key Set (JWKS) for token verification."
    )
    @GetMapping("/$JWKS", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun jwks(): Map<String, Any> {
        return jwkSet.toJSONObject()
    }
}
