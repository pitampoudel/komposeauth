package pitampoudel.komposeauth.user.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.web.savedrequest.SavedRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class LoginSuccessController {

    @GetMapping("/login/success")
    fun handleLoginSuccess(request: HttpServletRequest): String {
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