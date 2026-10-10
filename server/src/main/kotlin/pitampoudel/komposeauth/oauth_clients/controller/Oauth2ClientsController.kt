package pitampoudel.komposeauth.oauth_clients.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import pitampoudel.core.data.MessageResponse
import pitampoudel.komposeauth.core.domain.ApiEndpoints.OAUTH2_CLIENTS
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

    // Writes are a SUPER_ADMIN's: saving over a client without its secret issues a new one, and the
    // app still holding the old one stops signing anybody in.
    @Operation(
        summary = "Save OAuth2 client",
        description = "Registers a new OAuth2 client or updates existing. Requires SUPER_ADMIN."
    )
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun createClient(@RequestBody request: CreateClientRequest): ResponseEntity<OAuth2ClientResponse> {
        if (request.publicClient && request.clientSecret != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A public app has no client secret")
        }
        val obj = request.toEntity()
        oauth2ClientRepository.save(obj)
        return ResponseEntity.ok(obj.toClientRegistrationResponse())
    }

    @Operation(
        summary = "Delete OAuth2 client",
        description = "Deletes a registered OAuth2 client. Requires SUPER_ADMIN."
    )
    @DeleteMapping("/{clientId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun deleteClient(@PathVariable clientId: String): ResponseEntity<MessageResponse> {
        if (!oauth2ClientRepository.existsById(clientId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "OAuth2 client not found")
        }
        oauth2ClientRepository.deleteById(clientId)
        return ResponseEntity.ok(MessageResponse("OAuth2 client deleted"))
    }
}
