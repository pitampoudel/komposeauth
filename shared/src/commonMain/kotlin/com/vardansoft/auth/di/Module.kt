package com.vardansoft.auth.di

import com.vardansoft.auth.data.AuthClientImpl
import com.vardansoft.auth.data.LoginPreferencesImpl
import com.vardansoft.auth.domain.AuthClient
import com.vardansoft.auth.domain.LoginPreferences
import com.vardansoft.auth.domain.use_cases.ValidateOtpCode
import com.vardansoft.auth.presentation.LoginViewModel
import com.vardansoft.auth.presentation.otp.OtpViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    single<LoginPreferences> {
        LoginPreferencesImpl(get(), get())
    }
    viewModel<LoginViewModel> {
        LoginViewModel(get(), get())
    }
    single<ValidateOtpCode> {
        ValidateOtpCode()
    }
    viewModel<OtpViewModel> {
        OtpViewModel(get(), get(), get())
    }
    single<AuthClient> {
        AuthClientImpl(get())
    }
}