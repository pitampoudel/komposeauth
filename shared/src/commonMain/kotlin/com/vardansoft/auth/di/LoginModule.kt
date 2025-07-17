package com.vardansoft.auth.di

import com.vardansoft.auth.data.LoginClientImpl
import com.vardansoft.auth.data.LoginPreferencesImpl
import com.vardansoft.auth.domain.LoginClient
import com.vardansoft.auth.domain.LoginPreferences
import com.vardansoft.auth.presentation.LoginViewModel
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