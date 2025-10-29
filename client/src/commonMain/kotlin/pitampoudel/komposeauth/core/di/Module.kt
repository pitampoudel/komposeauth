package pitampoudel.komposeauth.core.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.AuthConfig
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pitampoudel.core.presentation.LazyState
import pitampoudel.komposeauth.core.data.AuthClientImpl
import pitampoudel.komposeauth.core.data.AuthPreferencesImpl
import pitampoudel.komposeauth.core.data.KtorBearerHandlerImpl
import pitampoudel.komposeauth.core.domain.AuthClient
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.core.domain.KtorBearerHandler
import pitampoudel.komposeauth.data.UserInfoResponse
import pitampoudel.komposeauth.kyc.KycViewModel
import pitampoudel.komposeauth.login.LoginViewModel
import pitampoudel.komposeauth.otp.OtpViewModel
import pitampoudel.komposeauth.profile.ProfileViewModel

fun initializeKomposeAuth(koinApp: KoinApplication?, authUrl: String, hosts: List<String>) {
    val module = module {
        single<ObservableSettings> { Settings().makeObservable() }
        single<AuthPreferences> { AuthPreferencesImpl(get()) }
        single<KtorBearerHandler> {
            KtorBearerHandlerImpl(
                authPreferences = get<AuthPreferences>(),
                authUrl = authUrl,
                serverUrls = hosts
            )
        }
    }
    koinApp?.modules(module) ?: run {
        startKoin {
            modules(module)
        }
    }
}

fun initializeKomposeAuthViewModels(httpClient: HttpClient) {
    loadKoinModules(
        module {
            single<AuthClient> {
                AuthClientImpl(
                    httpClient,
                    KomposeKoinComponent.getKoin().get<KtorBearerHandler>().authUrl
                )
            }
            viewModel<OtpViewModel> { OtpViewModel(get(), get()) }
            viewModel<LoginViewModel> { LoginViewModel(get(), get()) }
            viewModel<KycViewModel> { KycViewModel(get()) }
            viewModel<ProfileViewModel> { ProfileViewModel(get(), get()) }
        }
    )
}

private object KomposeKoinComponent : KoinComponent

fun setupBearerAuth(config: AuthConfig) {
    KomposeKoinComponent.getKoin().get<KtorBearerHandler>().configure(config)
}

@Composable
fun rememberCurrentUser(): LazyState<UserInfoResponse> {
    return koinInject<AuthPreferences>().userInfoResponse.map {
        LazyState.Loaded(it)
    }.collectAsStateWithLifecycle(LazyState.Loading).value
}