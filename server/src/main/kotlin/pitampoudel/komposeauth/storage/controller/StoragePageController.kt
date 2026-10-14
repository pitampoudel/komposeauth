package pitampoudel.komposeauth.storage.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import pitampoudel.komposeauth.core.controller.AdminShell

/** The admin console's Storage page; its data comes from [StorageController]. */
@Controller
class StoragePageController(
    private val adminShell: AdminShell
) {

    @GetMapping("/admin/storage")
    @PreAuthorize("hasRole('ADMIN')")
    fun storage(model: Model, authentication: Authentication): String {
        adminShell.apply(model)
        // The same test `StorageController` puts on a write, so no button leads to a 403.
        model.addAttribute("canManageStorage", authentication.authorities.any { it.authority == "ROLE_SUPER_ADMIN" })
        return "admin/storage"
    }
}
