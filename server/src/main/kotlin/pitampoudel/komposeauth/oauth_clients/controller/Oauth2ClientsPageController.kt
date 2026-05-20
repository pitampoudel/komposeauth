package pitampoudel.komposeauth.oauth_clients.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/oauth2/clients/dashboard")
class Oauth2ClientsPageController {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun page(): String {
        return "oauth2-clients"
    }
}
