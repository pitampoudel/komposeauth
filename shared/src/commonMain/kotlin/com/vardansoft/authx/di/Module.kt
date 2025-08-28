package com.vardansoft.authx.di

import com.vardansoft.authx.data.AuthClientImpl
import com.vardansoft.authx.data.AuthXImpl
import com.vardansoft.authx.data.AuthXPreferencesImpl
import com.vardansoft.authx.domain.AuthClient
import com.vardansoft.authx.domain.AuthX
import com.vardansoft.authx.domain.AuthXPreferences
import com.vardansoft.authx.domain.use_cases.ValidateOtpCode
import org.koin.dsl.module

fun authXSharedModule(authUrl: String, clientId: String, serverUrls: List<String>) = module {
    single<AuthXPreferences> {
        AuthXPreferencesImpl(get(), get())
    }
    single<ValidateOtpCode> {
        ValidateOtpCode()
    }
    single<AuthX> {
        AuthXImpl(
            authXPreferences = get<AuthXPreferences>(),
            authUrl = authUrl,
            clientId = clientId,
            serverUrls = serverUrls
        )
    }
    single<AuthClient> {
        AuthClientImpl(get(), authUrl)
    }
}