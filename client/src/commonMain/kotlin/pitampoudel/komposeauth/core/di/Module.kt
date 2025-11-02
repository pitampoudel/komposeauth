package pitampoudel.komposeauth.core.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pitampoudel.core.presentation.LazyState
import pitampoudel.komposeauth.core.data.AuthClientImpl
import pitampoudel.komposeauth.core.data.AuthPreferencesImpl
import pitampoudel.komposeauth.core.domain.Config
import pitampoudel.komposeauth.core.domain.AuthClient
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.data.ProfileResponse
import pitampoudel.komposeauth.kyc.KycViewModel
import pitampoudel.komposeauth.login.LoginViewModel
import pitampoudel.komposeauth.otp.OtpViewModel
import pitampoudel.komposeauth.profile.ProfileViewModel

/**
 * Initialize the KomposeAuth dependencies.
 * @param app The Koin application.
 * @param httpClient The http client with komposeauth installed
 */
fun initializeKomposeAuth(
    app: KoinApplication? = null,
    httpClient: HttpClient
) {
    val module = module {
        single<AuthPreferences> { AuthPreferencesImpl.instance }
        single<AuthClient> {
            val authServerUrl = Config.authServerUrl
            if (authServerUrl == null) {
                throw Exception("The provided http client must have komposeauth installed")
            }
            AuthClientImpl(httpClient = httpClient, authUrl = authServerUrl)
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
fun rememberCurrentUser(): LazyState<ProfileResponse> {
    val authPreferences = koinInject<AuthPreferences>()
    return produceState<LazyState<ProfileResponse>>(LazyState.Loading) {
        authPreferences.authenticatedUser.collectLatest { user ->
            value = LazyState.Loaded(user)
        }
    }.value
}