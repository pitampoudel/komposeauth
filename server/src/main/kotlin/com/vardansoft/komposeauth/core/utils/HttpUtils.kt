package com.vardansoft.komposeauth.core.utils

import jakarta.servlet.http.HttpServletRequest

fun HttpServletRequest.acceptsHtml(): Boolean {
    return getHeader("Accept")?.contains("text/html") == true
}
