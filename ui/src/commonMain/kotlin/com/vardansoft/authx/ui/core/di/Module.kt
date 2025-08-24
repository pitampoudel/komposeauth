package com.vardansoft.authx.ui.core.di

import com.vardansoft.authx.di.authSharedModule
import com.vardansoft.authx.ui.login.LoginViewModel
import com.vardansoft.authx.ui.otp.OtpViewModel
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