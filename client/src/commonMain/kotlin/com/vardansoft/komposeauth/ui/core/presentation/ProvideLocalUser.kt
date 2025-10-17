package com.vardansoft.komposeauth.ui.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vardansoft.komposeauth.data.UserInfoResponse
import com.vardansoft.komposeauth.ui.core.domain.AuthPreferences
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

val LocalUserState = compositionLocalOf<LazyState<UserInfoResponse>> {
    LazyState.Loading
}

@Composable
fun ProvideLocalUser(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        value = LocalUserState provides koinInject<AuthPreferences>().userInfoResponse.map {
            LazyState.Loaded(it)
        }.collectAsStateWithLifecycle(LazyState.Loading).value,
        content = content
    )
}