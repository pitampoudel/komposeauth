package com.vardansoft.authx.di

import com.vardansoft.authx.data.AuthClientImpl
import com.vardansoft.authx.data.LoginPreferencesImpl
import com.vardansoft.authx.domain.AuthClient
import com.vardansoft.authx.domain.LoginPreferences
import com.vardansoft.authx.domain.use_cases.ValidateOtpCode
import org.koin.dsl.module

fun authSharedModule(authUrl: String) = module {
    single<LoginPreferences> {
        LoginPreferencesImpl(get(), get())
    }
    single<ValidateOtpCode> {
        ValidateOtpCode()
    }
    single<AuthClient> {
        AuthClientImpl(get(), authUrl)
    }
}