package com.vardansoft.auth.oauth_clients.controller

import com.vardansoft.auth.oauth_clients.dto.CreateClientRequest
import com.vardansoft.auth.oauth_clients.dto.OAuth2ClientResponse
import com.vardansoft.auth.oauth_clients.dto.toClientRegistrationResponse
import com.vardansoft.auth.oauth_clients.dto.toEntity
import com.vardansoft.auth.oauth_clients.repository.OAuth2ClientRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/oauth2/clients")
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
