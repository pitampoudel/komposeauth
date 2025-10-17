package com.vardansoft.komposeauth.core.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import com.vardansoft.komposeauth.core.data.AuthClientImpl
import com.vardansoft.komposeauth.core.data.KtorBearerHandlerImpl
import com.vardansoft.komposeauth.core.data.AuthPreferencesImpl
import com.vardansoft.komposeauth.core.domain.KtorBearerHandler
import com.vardansoft.komposeauth.core.domain.AuthClient
import com.vardansoft.komposeauth.core.domain.AuthPreferences
import com.vardansoft.komposeauth.kyc.KycViewModel
import com.vardansoft.komposeauth.login.LoginViewModel
import com.vardansoft.komposeauth.otp.OtpViewModel
import com.vardansoft.komposeauth.profile.ProfileViewModel
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun KoinApplication.configureKomposeauth(authUrl: String, hosts: List<String>) {
    modules(
        module {
            single<ObservableSettings> {
                Settings().makeObservable()
            }
            single<AuthPreferences> {
                AuthPreferencesImpl(get())
            }
            single<KtorBearerHandler> {
                KtorBearerHandlerImpl(
                    authPreferences = get<AuthPreferences>(),
                    authUrl = authUrl,
                    serverUrls = hosts
                )
            }
            single<AuthClient> {
                AuthClientImpl(get(), authUrl)
            }
            viewModel<OtpViewModel> {
                OtpViewModel(get(), get())
            }
            viewModel<LoginViewModel> {
                LoginViewModel(get(), get())
            }
            viewModel<KycViewModel> {
                KycViewModel(get())
            }
            viewModel<ProfileViewModel> {
                ProfileViewModel(get(), get())
            }
        }
    )
}