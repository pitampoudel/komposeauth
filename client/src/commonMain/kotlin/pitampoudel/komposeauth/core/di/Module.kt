package pitampoudel.komposeauth.core.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.AuthConfig
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
import pitampoudel.komposeauth.core.data.KtorBearerHandlerImpl
import pitampoudel.komposeauth.core.domain.AuthClient
import pitampoudel.komposeauth.core.domain.AuthPreferences
import pitampoudel.komposeauth.core.domain.KtorBearerHandler
import pitampoudel.komposeauth.data.UserInfoResponse
import pitampoudel.komposeauth.kyc.KycViewModel
import pitampoudel.komposeauth.login.LoginViewModel
import pitampoudel.komposeauth.otp.OtpViewModel
import pitampoudel.komposeauth.profile.ProfileViewModel

/**
 * Initialize the core KomposeAuth dependencies.
 * Should be called *before* creating HttpClient since KtorBearerHandler may be required by it.
 */
fun initializeKomposeAuth(
    app: KoinApplication? = null,
    authUrl: String,
    hosts: List<String>
) {
    val coreModule = module {
        single<ObservableSettings> { Settings().makeObservable() }
        single<AuthPreferences> { AuthPreferencesImpl(get()) }
        single<KtorBearerHandler> {
            KtorBearerHandlerImpl(
                authPreferences = get(),
                authUrl = authUrl,
                serverUrls = hosts
            )
        }
    }
    app?.let {
        app.modules(coreModule)
    } ?: run {
        startKoin { modules(coreModule) }
    }
}

/**
 * Initialize ViewModel layer dependencies that depend on HttpClient.
 */
fun initializeKomposeAuthViewModels(app: KoinApplication?, httpClient: HttpClient) {
    val module = module {
        single<AuthClient> {
            AuthClientImpl(
                httpClient = httpClient,
                authUrl = get<KtorBearerHandler>().authUrl
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
        loadKoinModules(module)
    }
}

/**
 * Enable authentication at http client
 */
fun setupBearerAuth(config: AuthConfig) {
    KomposeKoinComponent.getKoin().get<KtorBearerHandler>().configure(config)
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