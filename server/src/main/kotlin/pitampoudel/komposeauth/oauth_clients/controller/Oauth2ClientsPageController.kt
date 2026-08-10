package pitampoudel.komposeauth.oauth_clients.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * The client list moved into the admin console at `/admin/clients`. This keeps the old address
 * working for anything that bookmarked it.
 */
@Controller
@RequestMapping("/oauth2/clients/dashboard")
class Oauth2ClientsPageController {

    @GetMapping
    fun page(): String = "redirect:/admin/clients"
}
