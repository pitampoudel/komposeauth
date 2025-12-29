package pitampoudel.komposeauth.login.presentation

import androidx.compose.runtime.Composable
import kotlinx.serialization.json.JsonObject
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.user.data.Credential
import pitampoudel.komposeauth.core.data.LoginOptionsResponse

interface KmpCredentialManager {
    suspend fun getCredential(options: LoginOptionsResponse): Result<Credential>
    suspend fun createPassKeyAndRetrieveJson(options: String): Result<JsonObject>
}

@Composable
expect fun rememberKmpCredentialManager(): KmpCredentialManager