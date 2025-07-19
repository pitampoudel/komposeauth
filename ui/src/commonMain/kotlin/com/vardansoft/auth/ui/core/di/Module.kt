package com.vardansoft.auth.ui.core.di

import com.vardansoft.auth.di.authModule
import com.vardansoft.auth.presentation.LoginViewModel
import com.vardansoft.auth.ui.otp.OtpViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun getAuthModules() = listOf(authModule) + module {
    viewModel<OtpViewModel> {
        OtpViewModel(get(), get(), get())
    }
    viewModel<LoginViewModel> {
        LoginViewModel(get(), get())
    }
}