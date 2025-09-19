package com.vardansoft.authx.oauth_clients.controller

import com.vardansoft.authx.oauth_clients.dto.CreateClientRequest
import com.vardansoft.authx.oauth_clients.dto.OAuth2ClientResponse
import com.vardansoft.authx.oauth_clients.dto.toClientRegistrationResponse
import com.vardansoft.authx.oauth_clients.dto.toEntity
import com.vardansoft.authx.oauth_clients.repository.OAuth2ClientRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@RestController
@RequestMapping("/oauth2/clients")
@ConditionalOnProperty(name = ["app.oauth-enabled"], havingValue = "true")
class Oauth2ClientsController(
    val oauth2ClientRepository: OAuth2ClientRepository
) {

    @GetMapping
    fun getAllClients(): ResponseEntity<List<OAuth2ClientResponse>> {
        return ResponseEntity.ok(
            oauth2ClientRepository.findAll().map {
                it.toClientRegistrationResponse()
            }
        )
    }

    @PostMapping
    fun createClient(@RequestBody request: CreateClientRequest): ResponseEntity<OAuth2ClientResponse> {
        val obj = request.toEntity()
        oauth2ClientRepository.save(obj)
        return ResponseEntity.ok(obj.toClientRegistrationResponse())
    }


}
