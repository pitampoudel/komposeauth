package pitampoudel.komposeauth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import pitampoudel.core.domain.Result
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.core.presentation.LazyState
import pitampoudel.core.presentation.wrapper.screenstate.ScreenStateWrapper
import pitampoudel.komposeauth.core.di.rememberCurrentUser
import pitampoudel.komposeauth.data.Credential
import pitampoudel.komposeauth.login.LoginEvent
import pitampoudel.komposeauth.login.LoginState
import pitampoudel.komposeauth.login.LoginViewModel
import pitampoudel.komposeauth.login.rememberKmpCredentialManager
import pitampoudel.komposeauth.profile.ProfileEvent
import pitampoudel.komposeauth.profile.ProfileState
import pitampoudel.komposeauth.profile.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    MaterialTheme {
        val userState = rememberCurrentUser()
        if (userState is LazyState.Loading) {
            return@MaterialTheme Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        val isLoggedIn = userState is LazyState.Loaded && userState.value != null

        if (!isLoggedIn) {
            val vm = koinViewModel<LoginViewModel>()
            LoginScreen(
                state = vm.state.collectAsStateWithLifecycle().value,
                onEvent = vm::onEvent,
            )
        } else {
            val vm = koinViewModel<ProfileViewModel>()
            ProfileScreen(
                state = vm.state.collectAsStateWithLifecycle().value,
                onEvent = vm::onEvent
            )
        }

    }
}

@Composable
private fun LoginScreen(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit,
) {
    val credentialManager = rememberKmpCredentialManager()
    val scope = rememberCoroutineScope()
    ScreenStateWrapper(
        progress = state.progress,
        infoMessage = state.infoMsg,
        onDismissInfoMsg = {
            onEvent(LoginEvent.DismissInfoMsg)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Welcome to KomposeAuth demo")
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    scope.launch {
                        state.loginConfig?.let { config ->
                            when (val result = credentialManager.getCredential(config)) {
                                is Result.Error -> onEvent(
                                    LoginEvent.ShowInfoMsg(result.message)
                                )

                                is Result.Success<Credential> -> onEvent(
                                    LoginEvent.Login(result.data)
                                )
                            }
                        } ?: run {
                            onEvent(LoginEvent.ShowInfoMsg(InfoMessage.General("Login options not available yet. Please try again.")))
                        }
                    }

                }) { Text("Sign in") }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
) {
    ScreenStateWrapper(
        progress = state.progress,
        infoMessage = state.infoMsg,
        onDismissInfoMsg = {
            onEvent(ProfileEvent.InfoMsgChanged(null))
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Text("You're signed in")
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    onEvent(ProfileEvent.LogOut)
                }) { Text("Log out") }
            }
        }
    }
}