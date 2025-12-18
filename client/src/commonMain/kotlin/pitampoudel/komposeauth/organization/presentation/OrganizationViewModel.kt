package pitampoudel.komposeauth.organization.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pitampoudel.komposeauth.organization.domain.OrganizationsClient

class OrganizationViewModel(
    val client: OrganizationsClient,
) : ViewModel() {
    private val _state = MutableStateFlow(OrganizationState())
    val state = _state.asStateFlow()

    fun onEvent(event: OrganizationEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is OrganizationEvent.DismissInfoMsg -> _state.update {
                    it.copy(infoMsg = null)
                }

                is OrganizationEvent.Load -> {
                    val res = client.get(event.orgId)
                    _state.update {
                        it.copy(res = res)
                    }
                }
            }
        }

    }
}