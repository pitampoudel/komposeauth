package com.vardansoft.auth.login.di

import com.vardansoft.auth.login.data.LoginClientImpl
import com.vardansoft.auth.login.data.LoginPreferencesImpl
import com.vardansoft.auth.login.domain.LoginClient
import com.vardansoft.auth.login.domain.LoginPreferences
import com.vardansoft.auth.login.presentation.LoginViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val loginModule = module {
    single<LoginPreferences> {
        LoginPreferencesImpl(get(), get())
    }
    viewModel<LoginViewModel> {
        LoginViewModel(get(), get())
    }
    single<LoginClient> {
        LoginClientImpl(get())
    }
}