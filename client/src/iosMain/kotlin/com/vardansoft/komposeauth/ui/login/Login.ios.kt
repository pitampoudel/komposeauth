package com.vardansoft.komposeauth.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vardansoft.komposeauth.data.Credential
import com.vardansoft.core.domain.Result
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
actual fun rememberCredentialRetriever(): CredentialRetriever {
    val retriever = remember {
        object : NSObject(), ASAuthorizationControllerDelegateProtocol,
            ASAuthorizationControllerPresentationContextProvidingProtocol {
            private var onResult: (Result<Credential>) -> Unit = {}
            suspend fun getCredential(): Result<Credential> {
                return suspendCoroutine { continuation ->
                    onResult = { result ->
                        continuation.resume(result)
                    }
                    val appleIDProvider = ASAuthorizationAppleIDProvider()
                    val request = appleIDProvider.createRequest()
                    request.requestedScopes =
                        listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)

                    val authorizationController = ASAuthorizationController(listOf(request))
                    authorizationController.delegate = this
                    authorizationController.presentationContextProvider = this
                    authorizationController.performRequests()
                }
            }

            override fun authorizationController(
                controller: ASAuthorizationController,
                didCompleteWithAuthorization: ASAuthorization
            ) {
                val appleIDCredential =
                    didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
                if (appleIDCredential != null) {
                    val idToken =
                        appleIDCredential.identityToken?.base64EncodedStringWithOptions(0u) ?: ""
                    onResult(Result.Success(Credential.AppleId(idToken)))
                } else {
                    onResult(Result.Error("Apple ID credential is null"))
                }
            }

            override fun authorizationController(
                controller: ASAuthorizationController,
                didCompleteWithError: NSError
            ) {
                onResult(Result.Error(didCompleteWithError.localizedDescription))
            }

            override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): UIWindow {
                return UIApplication.sharedApplication.windows.first() as UIWindow
            }
        }
    }
    return object : CredentialRetriever {
        override suspend fun getCredential(): Result<Credential> {
            return retriever.getCredential()
        }
    }
}

