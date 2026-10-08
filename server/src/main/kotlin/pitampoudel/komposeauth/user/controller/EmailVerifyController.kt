package pitampoudel.komposeauth.user.controller

import io.swagger.v3.oas.annotations.Operation
import org.apache.coyote.BadRequestException
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.view.RedirectView
import pitampoudel.komposeauth.app_config.service.AppConfigService
import pitampoudel.komposeauth.core.domain.ApiEndpoints.VERIFY_EMAIL
import pitampoudel.komposeauth.one_time_token.entity.OneTimeToken
import pitampoudel.komposeauth.one_time_token.service.OneTimeTokenService
import pitampoudel.komposeauth.user.service.UserService

@Controller
class EmailVerifyController(
    private val oneTimeTokenService: OneTimeTokenService,
    private val userService: UserService,
    private val appConfigService: AppConfigService
) {
    @Operation(
        summary = "Verify email address (link)",
        description = "link-based email verification using one-time token."
    )
    @GetMapping("/$VERIFY_EMAIL")
    fun verifyEmail(@RequestParam("token") token: String): RedirectView {
        val stored = oneTimeTokenService.consume(token, OneTimeToken.Purpose.VERIFY_EMAIL)
        val user = userService.findUser(stored.userId.toHexString())
            ?: throw BadRequestException("User not found")

        // Verify the address the link was sent to, which is not necessarily the one the account
        // holds now. Reading `user.email` here instead meant the link proved nothing about the
        // address it ended up marking verified: change the address after the mail is sent and
        // `User.update` correctly drops `emailVerified`, but clicking the old link put it straight
        // back — now attached to an address whose mailbox nobody had demonstrated reaching.
        val address = stored.subject
            ?: throw BadRequestException(STALE_LINK_MESSAGE)
        if (!address.equals(user.email, ignoreCase = true)) {
            throw BadRequestException(STALE_LINK_MESSAGE)
        }

        userService.markEmailVerified(user, address)

        val landing = appConfigService.getConfig().websiteUrl?.takeIf { it.isNotBlank() } ?: "/session-login"
        return RedirectView("$landing?emailVerified=true")
    }

    private companion object {
        /**
         * Covers both a link whose address has since been replaced and one issued before the address
         * was recorded on the token at all. The second only exists for as long as the tokens
         * outstanding at deploy time take to expire, and the alternative — trusting the account's
         * current address when the token does not name one — is the behaviour being fixed.
         */
        const val STALE_LINK_MESSAGE =
            "This verification link was sent to a different address than the one on your account now. " +
                    "Request a new one."
    }


}
