package com.vardansoft.auth.ui.core.di

import com.vardansoft.auth.di.authSharedModule
import com.vardansoft.auth.presentation.LoginViewModel
import com.vardansoft.auth.ui.otp.OtpViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun getAuthModules(authUrl: String) = listOf(authSharedModule(authUrl)) + module {
    viewModel<OtpViewModel> {
        OtpViewModel(get(), get(), get())
    }
    viewModel<LoginViewModel> {
        LoginViewModel(get(), get())
    }
}