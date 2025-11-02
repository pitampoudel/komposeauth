package pitampoudel.komposeauth.core.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.collectLatest
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
import pitampoudel.komposeauth.core.domain.AuthClient
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.data.UserInfoResponse
import pitampoudel.komposeauth.kyc.KycViewModel
import pitampoudel.komposeauth.login.LoginViewModel
import pitampoudel.komposeauth.otp.OtpViewModel
import pitampoudel.komposeauth.profile.ProfileViewModel

/**
 * Initialize the core KomposeAuth dependencies.
 */
fun initializeKomposeAuth(
    app: KoinApplication? = null,
    httpClient: HttpClient,
    authUrl: String
) {
    val coreModule = module {
        single<ObservableSettings> { Settings().makeObservable() }
        single<AuthPreferences> { AuthPreferencesImpl(get()) }
    }

    val module = module {
        single<AuthClient> {
            AuthClientImpl(
                httpClient = httpClient,
                authUrl = authUrl
            )
        }
        viewModel<OtpViewModel> { OtpViewModel(get(), get()) }
        viewModel<LoginViewModel> { LoginViewModel(get(), get()) }
        viewModel<KycViewModel> { KycViewModel(get()) }
        viewModel<ProfileViewModel> { ProfileViewModel(get(), get()) }
    }

    app?.let {
        app.modules(coreModule, module)
    } ?: run {
        startKoin { modules(coreModule, module) }
    }
}

/**
 * Observe current authenticated user reactively in Compose.
 */
@Composable
fun rememberCurrentUser(): LazyState<UserInfoResponse> {
    val authPreferences = koinInject<AuthPreferences>()
    return produceState<LazyState<UserInfoResponse>>(LazyState.Loading) {
        authPreferences.userInfoResponse.collectLatest { user ->
            value = LazyState.Loaded(user)
        }
    }.value
}

private object KomposeKoinComponent : KoinComponent