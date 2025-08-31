package com.vardansoft.authx.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vardansoft.authx.data.UserInfoResponse
import com.vardansoft.authx.domain.LazyState
import com.vardansoft.authx.domain.AuthXPreferences
import org.koin.compose.koinInject

val LocalUserInfoResponseState = compositionLocalOf<LazyState<UserInfoResponse>> { LazyState.Loading }

@Composable
fun ProvideUserInfo(content: @Composable () -> Unit) {
    val authXPreferences = koinInject<AuthXPreferences>()
    val userInfoState = authXPreferences.userInfoResponse.collectAsStateWithLifecycle(LazyState.Loading).value
    CompositionLocalProvider(
        LocalUserInfoResponseState provides userInfoState,
        content = content
    )
}