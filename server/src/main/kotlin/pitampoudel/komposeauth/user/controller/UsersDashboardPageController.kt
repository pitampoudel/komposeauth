package pitampoudel.komposeauth.user.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import pitampoudel.komposeauth.core.domain.ApiEndpoints.USERS

/**
 * The people list moved into the admin console at `/admin/users`. This keeps the old address
 * working for anything that bookmarked it.
 */
@Controller
@RequestMapping("/$USERS/dashboard")
class UsersDashboardPageController {

    @GetMapping
    fun page(): String = "redirect:/admin/users"
}
