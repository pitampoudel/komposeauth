package pitampoudel.komposeauth.login.presentation

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import kotlinx.serialization.json.JsonObject
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.core.data.LoginOptionsResponse
import pitampoudel.komposeauth.user.data.Credential
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.Foundation.NSError
import platform.Foundation.base64EncodedStringWithOptions
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
actual fun rememberKmpCredentialManager(): KmpCredentialManager {
    return object : KmpCredentialManager {
        override suspend fun getCredential(options: LoginOptionsResponse): Result<Credential> {
            return AppleSignInHelper().getCredential()
        }

        override suspend fun createPassKeyAndRetrieveJson(options: String): Result<JsonObject> {
            return Result.Error("Passkeys are not implemented on iOS yet")
        }
    }
}

class AppleSignInHelper : NSObject(),
    ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {

    private var onResult: (Result<Credential>) -> Unit = {}

    // Keep controller strongly too (helps debugging)
    private var authController: ASAuthorizationController? = null

    suspend fun getCredential(): Result<Credential> = suspendCoroutine { cont ->
        Logger.d("Starting Apple sign in flow")
        onResult = { r ->
            authController = null
            Logger.v("Apple sign in result received")
            cont.resume(r)
        }

        val provider = ASAuthorizationAppleIDProvider()
        val request = provider.createRequest()
        request.requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)

        val controller = ASAuthorizationController(listOf(request))
        authController = controller
        controller.delegate = this
        controller.presentationContextProvider = this
        controller.performRequests()
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization
    ) {
        Logger.d("Apple sign in completed successfully")
        val cred = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        if (cred == null) {
            onResult(Result.Error("Apple ID credential is null"))
            return
        }
        val idToken = cred.identityToken?.base64EncodedStringWithOptions(0u).orEmpty()
        onResult(Result.Success(Credential.AppleId(idToken)))
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError
    ) {
        Logger.e("Apple sign in failed: code=${didCompleteWithError.code}, ${didCompleteWithError.localizedDescription}")
        onResult(Result.Error("Apple auth failed: code=${didCompleteWithError.code}, ${didCompleteWithError.localizedDescription}"))
    }

    override fun presentationAnchorForAuthorizationController(
        controller: ASAuthorizationController
    ): UIWindow {
        val app = UIApplication.sharedApplication
        val key = app.keyWindow
        if (key != null) return key

        // Fallback (older iOS / edge cases)
        return (app.windows.firstOrNull() ?: error("No UIWindow available")) as UIWindow
    }

}


