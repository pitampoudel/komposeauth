package pitampoudel.komposeauth.login

import pitampoudel.core.domain.Result
import androidx.compose.runtime.Composable
import kotlinx.serialization.json.JsonObject
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.data.LoginOptions

interface KmpCredentialManager {
    suspend fun getCredential(options: LoginOptions): Result<Credential>
    suspend fun createPassKeyAndRetrieveJson(options: String): Result<JsonObject>
}

@Composable
expect fun rememberKmpCredentialManager(): KmpCredentialManager