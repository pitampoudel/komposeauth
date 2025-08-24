package com.vardansoft.authx.credentials.controller

import com.vardansoft.authx.core.config.UserContextService
import com.vardansoft.authx.credentials.dto.CredentialResponse
import com.vardansoft.authx.credentials.dto.UpdateCredentialRequest
import com.vardansoft.authx.credentials.service.CredentialService
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/credentials")
class CredentialController(
    private val credentialService: CredentialService,
    private val userContextService: UserContextService
) {

    @GetMapping
    fun getMyCredentials(): ResponseEntity<List<CredentialResponse>> {
        val user = userContextService.getCurrentUser()
        val credentials = credentialService.findCredentials(user.id)
        return ResponseEntity.ok(credentials)
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('SCOPE_user.read.any')")
    fun getCredentials(
        @PathVariable userId: String
    ): ResponseEntity<List<CredentialResponse>> {
        val credentials = credentialService.findCredentials(ObjectId(userId))
        return ResponseEntity.ok(credentials)
    }

    @PreAuthorize("hasAuthority('SCOPE_user.write.any')")
    @PostMapping("/{userId}")
    fun saveCredential(
        @PathVariable userId: String,
        @RequestBody data: UpdateCredentialRequest
    ): ResponseEntity<*> {
        credentialService.updateCredential(ObjectId(userId), data)
        return ResponseEntity.ok().build<String>()
    }
}