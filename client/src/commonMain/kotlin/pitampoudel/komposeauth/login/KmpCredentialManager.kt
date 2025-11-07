package pitampoudel.komposeauth.login

import pitampoudel.core.domain.Result
import androidx.compose.runtime.Composable
import kotlinx.serialization.json.JsonObject
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.LoginOptionsResponse

interface KmpCredentialManager {
    suspend fun getCredential(options: LoginOptionsResponse): Result<Credential>
    suspend fun createPassKeyAndRetrieveJson(options: String): Result<JsonObject>
}

@Composable
expect fun rememberKmpCredentialManager(): KmpCredentialManager