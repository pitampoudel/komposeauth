package com.vardansoft.auth.ui.login


import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.vardansoft.auth.VardanSoftAuth
import com.vardansoft.auth.presentation.Credential

@Composable
actual fun rememberCredentialRetriever(clientId: String): CredentialRetriever {
    val activity = LocalActivity.current ?: throw Exception("No activity found")
    val credentialManager = remember {
        CredentialManager.create(activity)
    }
    return remember {
        object : CredentialRetriever {
            override suspend fun getCredential(): Result<Credential> {
                val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(VardanSoftAuth.GOOGLE_ID)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                try {
                    val result = credentialManager.getCredential(
                        context = activity,
                        request = request
                    )
                    val credential: Result<Credential> = when (val credential = result.credential) {
                        is CustomCredential -> {
                            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                try {
                                    val googleIdTokenCredential =
                                        GoogleIdTokenCredential.createFrom(credential.data)
                                    Result.success(
                                        Credential.GoogleId(
                                            clientId = clientId,
                                            idToken = googleIdTokenCredential.idToken
                                        )
                                    )
                                } catch (e: GoogleIdTokenParsingException) {
                                    Result.failure(e)
                                }
                            } else {
                                Result.failure(Exception("Unexpected type of credential"))
                            }

                        }

                        else -> Result.failure(Exception("Unknown credential type"))
                    }
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    return credential
                } catch (e: GetCredentialCancellationException) {
                    e.printStackTrace()
                    return Result.failure(e)

                } catch (e: GetCredentialException) {
                    e.printStackTrace()
                    return Result.failure(e)
                }
            }

        }
    }

}