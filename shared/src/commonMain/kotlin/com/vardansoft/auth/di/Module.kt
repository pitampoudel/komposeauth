package com.vardansoft.auth.di

import com.vardansoft.auth.data.AuthClientImpl
import com.vardansoft.auth.data.LoginPreferencesImpl
import com.vardansoft.auth.domain.AuthClient
import com.vardansoft.auth.domain.LoginPreferences
import com.vardansoft.auth.domain.use_cases.ValidateOtpCode
import org.koin.dsl.module

val authSharedModule = module {
    single<LoginPreferences> {
        LoginPreferencesImpl(get(), get())
    }
    single<ValidateOtpCode> {
        ValidateOtpCode()
    }
    single<AuthClient> {
        AuthClientImpl(get())
    }
}