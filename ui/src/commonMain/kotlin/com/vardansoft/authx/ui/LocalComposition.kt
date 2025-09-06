package com.vardansoft.authx.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vardansoft.authx.data.UserInfoResponse
import com.vardansoft.authx.ui.core.LazyState
import com.vardansoft.authx.domain.AuthXPreferences
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

val LocalUserInfoResponseState = compositionLocalOf<LazyState<UserInfoResponse>> {
    LazyState.Loading
}

@Composable
fun ProvideUserInfo(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        value = LocalUserInfoResponseState provides koinInject<AuthXPreferences>().userInfoResponse.map {
            LazyState.Loaded(it)
        }.collectAsStateWithLifecycle(LazyState.Loading).value,
        content = content
    )
}