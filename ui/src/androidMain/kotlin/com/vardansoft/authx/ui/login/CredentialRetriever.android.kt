package com.vardansoft.authx.ui.login


import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.vardansoft.authx.data.Credential
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.core.domain.Result
import org.koin.java.KoinJavaComponent.getKoin

@Composable
actual fun rememberCredentialRetriever(): CredentialRetriever {
    val activity = LocalActivity.current ?: throw Exception("No activity found")
    val credentialManager = remember {
        CredentialManager.create(activity)
    }
    return remember {
        object : CredentialRetriever {
            override suspend fun getCredential(): Result<Credential> {
                // Fetch Google OAuth client-id dynamically from server
                val authXClient = getKoin().get<AuthXClient>()
                when (val res = authXClient.fetchConfig()) {
                    is Result.Error -> return res
                    is Result.Success -> {
                        val googleAuthClientId = res.data.googleClientId

                        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(googleAuthClientId)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        try {
                            val result = credentialManager.getCredential(
                                context = activity,
                                request = request
                            )
                            val credential: Result<Credential> =
                                when (val credential = result.credential) {
                                    is CustomCredential -> {
                                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            try {
                                                val googleIdTokenCredential =
                                                    GoogleIdTokenCredential.createFrom(credential.data)
                                                Result.Success(
                                                    Credential.GoogleId(
                                                        idToken = googleIdTokenCredential.idToken
                                                    )
                                                )
                                            } catch (e: GoogleIdTokenParsingException) {
                                                Result.Error(e.message.orEmpty())
                                            }
                                        } else {
                                            Result.Error("Unexpected type of credential")
                                        }

                                    }

                                    else -> Result.Error("Unknown credential type")
                                }
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                            return credential
                        } catch (e: GetCredentialCancellationException) {
                            e.printStackTrace()
                            return Result.Error(e.message.orEmpty())

                        } catch (e: GetCredentialException) {
                            e.printStackTrace()
                            val userMessage = when (e) {
                                is GetCredentialProviderConfigurationException ->
                                    "Google Play services on this device is missing, out of date, or not compatible to sign in with Google. Please update/install Google Play services and try again."

                                is NoCredentialException ->
                                    "No Google account available for sign-in on this device. Please add a Google account and try again."

                                else -> e.message
                                    ?: "Unable to retrieve credentials at the moment. Please try again."
                            }
                            return Result.Error(userMessage)
                        }
                    }
                }
            }

        }
    }

}