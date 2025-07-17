package com.vardansoft.auth.ui

import androidx.compose.runtime.compositionLocalOf
import com.vardansoft.auth.login.data.UserInfo

val LocalUserInfo = compositionLocalOf<UserInfo?> { null }