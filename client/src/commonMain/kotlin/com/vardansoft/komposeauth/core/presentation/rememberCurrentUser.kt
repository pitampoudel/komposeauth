package com.vardansoft.komposeauth.core.presentation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vardansoft.core.presentation.LazyState
import com.vardansoft.komposeauth.core.domain.AuthPreferences
import com.vardansoft.komposeauth.data.UserInfoResponse
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject


@Composable
fun rememberCurrentUser(): LazyState<UserInfoResponse> {
    return koinInject<AuthPreferences>().userInfoResponse.map {
        LazyState.Loaded(it)
    }.collectAsStateWithLifecycle(LazyState.Loading).value
}