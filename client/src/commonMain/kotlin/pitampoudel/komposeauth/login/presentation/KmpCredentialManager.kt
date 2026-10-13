package pitampoudel.komposeauth.login.presentation

import androidx.compose.runtime.Composable
import kotlinx.serialization.json.JsonObject
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.core.data.LoginOptionsResponse

enum class CredentialType {
    ANY,
    APPLE,
    GOOGLE
}

interface KmpCredentialManager {
    suspend fun getCredential(credentialType: CredentialType, options: LoginOptionsResponse): Result<Credential>

    /** Whether a passkey can be created here, so an app offers to add one only where it works. */
    val canCreatePasskeys: Boolean get() = true

    /** What the account's list of passkeys calls one created here: this device or browser. */
    val passkeyLabel: String get() = "Passkey"

    /**
     * Creates a passkey from [options], `PublicKeyCredentialCreationOptions` JSON, and answers the
     * new credential in the WebAuthn JSON shape, or [PasskeyCancelled] when the person closed the
     * prompt.
     */
    suspend fun createPassKeyAndRetrieveJson(options: String): Result<JsonObject>
}

/** The person closed the passkey prompt: nothing went wrong, so there is nothing to tell them. */
data object PasskeyCancelled : Result.Error("Passkey creation was cancelled")

@Composable
expect fun rememberKmpCredentialManager(): KmpCredentialManager
