package com.vardansoft.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vardansoft.auth.data.UserInfo
import com.vardansoft.auth.domain.LoginPreferences
import org.koin.compose.koinInject

val LocalUserInfo = compositionLocalOf<UserInfo?> { null }

@Composable
fun ProvideUserInfo(content: @Composable () -> Unit) {
    val preferences = koinInject<LoginPreferences>()
    CompositionLocalProvider(
        LocalUserInfo.provides(preferences.userInfo.collectAsStateWithLifecycle(
            null
        ).value),
        content = content
    )
}