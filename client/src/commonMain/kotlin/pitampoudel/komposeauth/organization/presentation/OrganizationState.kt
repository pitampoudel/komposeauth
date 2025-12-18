package pitampoudel.komposeauth.organization.presentation

import pitampoudel.core.domain.Result
import pitampoudel.core.presentation.InfoMessage
import pitampoudel.komposeauth.organization.data.OrganizationResponse

data class OrganizationState(
    val progress: Float? = null,
    val infoMsg: InfoMessage? = null,
    val res: Result<OrganizationResponse>? = null
)