package com.vardansoft.authx.ui.core.di

import com.vardansoft.authx.di.authXSharedModule
import com.vardansoft.authx.ui.login.LoginViewModel
import com.vardansoft.authx.ui.otp.OtpViewModel
import com.vardansoft.authx.ui.kyc.KycViewModel
import com.vardansoft.authx.ui.profile.ProfileViewModel
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun KoinApplication.configureAuthX(authUrl: String, hosts: List<String>) {
    modules(
        authXSharedModule(
            authUrl = authUrl,
            serverUrls = hosts
        ) + module {
            viewModel<OtpViewModel> {
                OtpViewModel(get(), get(), get())
            }
            viewModel<LoginViewModel> {
                LoginViewModel(get(), get())
            }
            viewModel<KycViewModel> {
                KycViewModel(get(), get(), get())
            }
            viewModel<ProfileViewModel> {
                ProfileViewModel(get(), get())
            }
        }
    )
}