package pitampoudel.komposeauth

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import pitampoudel.core.domain.Result
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.login.LoginEvent
import pitampoudel.komposeauth.login.LoginViewModel
import pitampoudel.komposeauth.login.rememberKmpCredentialManager

@Composable
@Preview
fun App() {
    MaterialTheme {
        val vm = koinViewModel<LoginViewModel>()
        val state = vm.state.collectAsStateWithLifecycle().value
        val credentialManager = rememberKmpCredentialManager()
        LaunchedEffect(state.loginConfig) {
            state.loginConfig?.let {
                when (val result = credentialManager.getCredential(it)) {
                    is Result.Error -> vm.onEvent(LoginEvent.ShowInfoMsg(result.message))
                    is Result.Success<Credential> -> vm.onEvent(LoginEvent.Login(result.data))
                }
            }
        }
    }
}