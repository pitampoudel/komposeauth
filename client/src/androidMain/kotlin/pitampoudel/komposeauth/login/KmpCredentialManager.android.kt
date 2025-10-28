package pitampoudel.komposeauth.login


import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.LoginOptions

@Composable
actual fun rememberKmpCredentialManager(): KmpCredentialManager {
    val activity = LocalActivity.current ?: throw Exception("No activity found")
    val credentialManager = remember {
        CredentialManager.create(activity)
    }
    return remember {
        object : KmpCredentialManager {
            override suspend fun getCredential(options: LoginOptions): Result<Credential> {

                val googleAuthClientId = options.googleClientId
                val publicKeyVerificationOptionsJson = options.publicKeyAuthOptionsJson

                val googleIdOption: GetGoogleIdOption? = googleAuthClientId?.let {
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(googleAuthClientId)
                        .build()
                }

                val request = GetCredentialRequest.Builder()
                    .also { req ->
                        googleIdOption?.let {
                            req.addCredentialOption(googleIdOption)
                        }
                        publicKeyVerificationOptionsJson?.let {
                            req.addCredentialOption(
                                GetPublicKeyCredentialOption(
                                    requestJson = publicKeyVerificationOptionsJson
                                )
                            )
                        }
                    }
                    .build()

                try {
                    val result = credentialManager.getCredential(
                        context = activity,
                        request = request
                    )
                    val credential: Result<Credential> =
                        when (val credential = result.credential) {
                            is PublicKeyCredential -> {
                                Result.Success(
                                    Credential.PublicKey(
                                        authenticationResponseJson = credential.authenticationResponseJson
                                    )
                                )
                            }

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

            override suspend fun createPassKeyAndRetrieveJson(options: String): Result<JsonObject> {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return Result.Error("Android version not supported")

                val response = try {
                    credentialManager.createCredential(
                        context = activity,
                        request = CreatePublicKeyCredentialRequest(options)
                    )

                } catch (e: CreateCredentialException) {
                    e.printStackTrace()
                    return Result.Error(e.message.orEmpty())
                }

                return when (response) {

                    is CreatePublicKeyCredentialResponse -> {
                        Result.Success(Json.parseToJsonElement(response.registrationResponseJson).jsonObject)
                    }

                    else -> {
                        Result.Error("Unknown credential")
                    }
                }
            }

        }
    }

}
