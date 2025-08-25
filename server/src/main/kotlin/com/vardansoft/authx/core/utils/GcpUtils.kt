package com.vardansoft.authx.core.utils

import com.google.cloud.ServiceOptions

fun getGcpProjectId(): String {
    return ServiceOptions.getDefaultProjectId()
}