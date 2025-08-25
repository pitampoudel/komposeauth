package com.vardansoft.authx.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vardansoft.authx.data.UserInfo
import com.vardansoft.authx.data.LazyState
import com.vardansoft.authx.domain.LoginPreferences
import org.koin.compose.koinInject

val LocalUserInfoState = compositionLocalOf<LazyState<UserInfo>> { LazyState.Loading }

@Composable
fun ProvideUserInfo(content: @Composable () -> Unit) {
    val preferences = koinInject<LoginPreferences>()
    val userInfoState = preferences.userInfo.collectAsStateWithLifecycle(LazyState.Loading).value
    CompositionLocalProvider(
        LocalUserInfoState provides userInfoState,
        content = content
    )
}