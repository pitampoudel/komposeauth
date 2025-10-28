package pitampoudel.komposeauth.core.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import pitampoudel.komposeauth.core.data.AuthClientImpl
import pitampoudel.komposeauth.core.data.KtorBearerHandlerImpl
import pitampoudel.komposeauth.core.data.AuthPreferencesImpl
import pitampoudel.komposeauth.core.domain.KtorBearerHandler
import pitampoudel.komposeauth.core.domain.AuthClient
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.kyc.KycViewModel
import pitampoudel.komposeauth.login.LoginViewModel
import pitampoudel.komposeauth.otp.OtpViewModel
import pitampoudel.komposeauth.profile.ProfileViewModel
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