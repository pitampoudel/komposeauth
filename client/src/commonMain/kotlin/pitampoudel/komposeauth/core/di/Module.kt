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
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.scope.Scope
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

internal fun komposeAuthModule(
    httpClient: HttpClient,
    authUrl: String,
    hosts: List<String>
): Module = module {
    // library-managed settings & http client
    single<ObservableSettings> { Settings().makeObservable() }

    // core services
    single<AuthClient> { AuthClientImpl(httpClient, authUrl) }
    single<AuthPreferences> { AuthPreferencesImpl(get()) }
    single<KtorBearerHandler> {
        KtorBearerHandlerImpl(
            authPreferences = get<AuthPreferences>(),
            authUrl = authUrl,
            serverUrls = hosts
        )
    }

    // ViewModels
    viewModel<OtpViewModel> { OtpViewModel(get(), get()) }
    viewModel<LoginViewModel> { LoginViewModel(get(), get()) }
    viewModel<KycViewModel> { KycViewModel(get()) }
    viewModel<ProfileViewModel> { ProfileViewModel(get(), get()) }
}


// Public entrypoint
fun initializeKomposeAuth(httpClient: HttpClient, authUrl: String, hosts: List<String>) {
    startKoin {
        modules(komposeAuthModule(httpClient = httpClient, authUrl, hosts))
    }
}

fun Scope.setupBearerAuth(config: AuthConfig) {
    val ktorBearerHandler = get<KtorBearerHandler>()
    ktorBearerHandler.configure(config)
}

@Composable
fun rememberCurrentUser(): LazyState<UserInfoResponse> {
    return koinInject<AuthPreferences>().userInfoResponse.map {
        LazyState.Loaded(it)
    }.collectAsStateWithLifecycle(LazyState.Loading).value
}