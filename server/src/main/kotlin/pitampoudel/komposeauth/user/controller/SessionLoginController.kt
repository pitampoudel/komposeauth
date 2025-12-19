package pitampoudel.komposeauth.user.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.savedrequest.SavedRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import pitampoudel.komposeauth.core.data.Credential
import pitampoudel.komposeauth.user.service.UserService

@Controller
class SessionLoginController(
    val userService: UserService,
) {

    @GetMapping("/session-login")
    fun sessionLoginPage(): String {
        return "session-login"
    }

    @PostMapping("/session-login")
    fun handleLoginSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        securityContextRepository: HttpSessionSecurityContextRepository,
        @RequestParam("username") username: String,
        @RequestParam("password") password: String
    ): String {
        val user = userService.resolveUserFromCredential(
            request = Credential.UsernamePassword(
                username = username,
                password = password
            ),
            loadPublicKeyCredentialRequestOptions = {
                null
            }
        )

        val authorities = user.roles.map { SimpleGrantedAuthority("ROLE_$it") }
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                user.id,
                null,
                authorities
            )
        securityContextRepository.saveContext(
            SecurityContextHolder.getContext(), request, response
        )

        val savedRequest = request.session
            ?.getAttribute("SPRING_SECURITY_SAVED_REQUEST") as? SavedRequest

        return if (savedRequest != null) {
            // Clear the saved request
            request.session.removeAttribute("SPRING_SECURITY_SAVED_REQUEST")
            "redirect:${savedRequest.redirectUrl}"
        } else {
            "redirect:/"
        }
    }
}