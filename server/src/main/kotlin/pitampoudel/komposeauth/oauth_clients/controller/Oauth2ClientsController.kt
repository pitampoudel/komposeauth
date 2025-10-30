package pitampoudel.komposeauth.oauth_clients.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pitampoudel.komposeauth.data.ApiEndpoints.OAUTH2_CLIENTS
import pitampoudel.komposeauth.oauth_clients.dto.CreateClientRequest
import pitampoudel.komposeauth.oauth_clients.dto.OAuth2ClientResponse
import pitampoudel.komposeauth.oauth_clients.dto.toClientRegistrationResponse
import pitampoudel.komposeauth.oauth_clients.dto.toEntity
import pitampoudel.komposeauth.oauth_clients.repository.OAuth2ClientRepository

@RestController
@RequestMapping("/$OAUTH2_CLIENTS")
@PreAuthorize("hasRole('ADMIN')")
class Oauth2ClientsController(
    val oauth2ClientRepository: OAuth2ClientRepository
) {

    @Operation(
        summary = "Get all OAuth2 clients",
        description = "Retrieves a list of all registered OAuth2 clients."
    )
    @GetMapping
    fun getAllClients(): ResponseEntity<List<OAuth2ClientResponse>> {
        return ResponseEntity.ok(
            oauth2ClientRepository.findAll().map {
                it.toClientRegistrationResponse()
            }
        )
    }

    @Operation(
        summary = "Create OAuth2 client",
        description = "Registers a new OAuth2 client."
    )
    @PostMapping
    fun createClient(@RequestBody request: CreateClientRequest): ResponseEntity<OAuth2ClientResponse> {
        val obj = request.toEntity()
        oauth2ClientRepository.save(obj)
        return ResponseEntity.ok(obj.toClientRegistrationResponse())
    }


}
