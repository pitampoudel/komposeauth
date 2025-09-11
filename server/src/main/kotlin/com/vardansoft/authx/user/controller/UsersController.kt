package com.vardansoft.authx.user.controller

import com.vardansoft.authx.core.service.EmailService
import com.vardansoft.authx.core.service.JwtService
import com.vardansoft.authx.core.utils.acceptsHtml
import com.vardansoft.authx.data.CreateUserRequest
import com.vardansoft.authx.user.dto.UserResponse
import com.vardansoft.authx.user.dto.mapToResponseDto
import com.vardansoft.authx.user.service.UserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam


@Controller
class UsersController(
    val userService: UserService,
    val emailService: EmailService,
    val jwtService: JwtService
) {

    @GetMapping("/signup")
    fun create(): String {
        return "signup"
    }

    @GetMapping("/login")
    fun login(): String {
        return "login"
    }

    @PostMapping("/users")
    fun create(
        @ModelAttribute request: CreateUserRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        val createdUser = userService.findOrCreateUser(request)
        val userResponse = createdUser.mapToResponseDto()
        if (createdUser.email != null && !createdUser.emailVerified) emailService.sendSimpleMail(
            to = createdUser.email,
            subject = "Email Verification",
            text = "Please click the link to verify your email address: ${
                jwtService.generateEmailVerificationLink(
                    userId = createdUser.id.toHexString()
                )
            }"
        )
        return if (httpRequest.acceptsHtml()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/login")
                .body("Redirecting to /login")
        } else {
            ResponseEntity.ok().body(userResponse)
        }
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user.read.any')")
    fun getUserById(@PathVariable id: String): ResponseEntity<UserResponse> {
        val user = userService.findUser(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user.mapToResponseDto())
    }

    @GetMapping("/users/batch")
    @PreAuthorize("hasAuthority('SCOPE_user.read.any')")
    fun getUsersBatch(@RequestParam ids: String): ResponseEntity<List<UserResponse>> {
        val userIds = ids.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (userIds.isEmpty()) {
            return ResponseEntity.badRequest().build()
        }

        val users = userService.findUsersBulk(userIds)
        val userResponses = users.map { it.mapToResponseDto() }
        return ResponseEntity.ok(userResponses)
    }

}
