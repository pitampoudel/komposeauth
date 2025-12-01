package pitampoudel.komposeauth.core.domain

import kotlinx.coroutines.flow.Flow
import pitampoudel.komposeauth.data.OAuth2Response

internal interface AuthPreferences {
    val tokenClaims: Flow<AccessTokenClaims?>
    suspend fun saveTokenData(tokenData: OAuth2Response)
    fun tokenData(): OAuth2Response?
    suspend fun clear()
}
