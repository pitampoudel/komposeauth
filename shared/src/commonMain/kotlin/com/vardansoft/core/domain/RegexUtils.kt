package com.vardansoft.core.domain

fun isUrlValid(url: String): Boolean {
    val pattern = "^((https?|ftp)://)?" + // Protocol
            "(([a-zA-Z0-9._-]+\\.[a-zA-Z]{2,6})|" + // Domain name
            "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))" + // OR IP address
            "(:[0-9]{1,4})?" + // Port
            "(/\\S*)?$" // Path
    return Regex(pattern).matches(url)
}