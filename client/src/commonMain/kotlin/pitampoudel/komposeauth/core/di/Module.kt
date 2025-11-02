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
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pitampoudel.core.presentation.LazyState
import pitampoudel.komposeauth.core.data.AuthClientImpl
import pitampoudel.komposeauth.core.data.AuthPreferencesImpl
import pitampoudel.komposeauth.core.data.AuthStateChecker
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

    val module = module {
        single<ObservableSettings> { Settings().makeObservable() }
        single<AuthStateChecker> { AuthStateChecker(httpClient = httpClient, authUrl = authUrl) }
        single<AuthPreferences> { AuthPreferencesImpl(get(), get()) }
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
        app.modules(module)
    } ?: run {
        startKoin { modules(module) }
    }
}

/**
 * Observe current authenticated user reactively in Compose.
 */
@Composable
fun rememberCurrentUser(): LazyState<UserInfoResponse> {
    val authPreferences = koinInject<AuthPreferences>()
    return produceState<LazyState<UserInfoResponse>>(LazyState.Loading) {
        authPreferences.authenticatedUserInfo.collectLatest { user ->
            value = LazyState.Loaded(user)
        }
    }.value
}

private object KomposeKoinComponent : KoinComponent