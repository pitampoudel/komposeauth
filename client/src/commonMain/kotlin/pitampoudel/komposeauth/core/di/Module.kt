package pitampoudel.komposeauth.core.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.ktor.client.HttpClient
import org.koin.compose.koinInject
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pitampoudel.core.presentation.LazyState
import pitampoudel.komposeauth.login.data.AuthClientImpl
import pitampoudel.komposeauth.login.data.AuthPreferencesImpl
import pitampoudel.komposeauth.core.data.AuthStateHandler
import pitampoudel.komposeauth.login.domain.AuthClient
import pitampoudel.komposeauth.login.domain.AuthPreferences
import pitampoudel.komposeauth.core.domain.AuthUser
import pitampoudel.komposeauth.core.domain.Config
import pitampoudel.komposeauth.organization.data.OrganizationsClientImpl
import pitampoudel.komposeauth.kyc.KycViewModel
import pitampoudel.komposeauth.login.domain.use_cases.LoginUser
import pitampoudel.komposeauth.login.presentation.LoginViewModel
import pitampoudel.komposeauth.organization.domain.OrganizationsClient
import pitampoudel.komposeauth.organization.presentation.CreateOrganizationViewModel
import pitampoudel.komposeauth.organization.presentation.OrganizationViewModel
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
        single<AuthPreferences> { AuthPreferencesImpl.getInstance() }
        single<AuthClient> {
            val authServerUrl = Config.authServerUrl
                ?: throw Exception("The provided http client must have komposeauth installed")
            AuthClientImpl(httpClient = httpClient, authUrl = authServerUrl)
        }
        single<OrganizationsClient> {
            val authServerUrl = Config.authServerUrl
                ?: throw Exception("The provided http client must have komposeauth installed")
            OrganizationsClientImpl(httpClient = httpClient, baseUrl = authServerUrl)
        }
        single { AuthStateHandler(get(), get()) }
        single { LoginUser(get(), get(), get()) }
        viewModel<OtpViewModel> { OtpViewModel(get(), get()) }
        viewModel<LoginViewModel> { LoginViewModel(get(), get()) }
        viewModel<KycViewModel> { KycViewModel(get()) }
        viewModel<ProfileViewModel> { ProfileViewModel(get(), get(), get()) }
        viewModel<CreateOrganizationViewModel> { CreateOrganizationViewModel(get()) }
        viewModel<OrganizationViewModel> { OrganizationViewModel(get()) }
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
fun rememberAuthenticatedUser(): LazyState<AuthUser> {
    val authPreferences = koinInject<AuthStateHandler>()
    return authPreferences.currentUser.collectAsStateWithLifecycle().value
}