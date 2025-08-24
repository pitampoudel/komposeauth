package com.vardansoft.authx.ui.core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.vardansoft.authx.domain.LoginPreferences
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

interface LogoutHandler {
    fun logout()
}

@Composable
fun rememberLogoutHandler(): LogoutHandler {
    val scope = rememberCoroutineScope()
    val loginPreferences = koinInject<LoginPreferences>()
    return remember {
        object : LogoutHandler {
            override fun logout() {
                scope.launch {
                    loginPreferences.clear()
                }
            }
        }

    }
}