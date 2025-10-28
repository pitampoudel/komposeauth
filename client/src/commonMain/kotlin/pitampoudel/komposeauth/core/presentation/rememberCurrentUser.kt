package pitampoudel.komposeauth.core.presentation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pitampoudel.core.presentation.LazyState
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.data.UserInfoResponse
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject


@Composable
fun rememberCurrentUser(): LazyState<UserInfoResponse> {
    return koinInject<AuthPreferences>().userInfoResponse.map {
        LazyState.Loaded(it)
    }.collectAsStateWithLifecycle(LazyState.Loading).value
}