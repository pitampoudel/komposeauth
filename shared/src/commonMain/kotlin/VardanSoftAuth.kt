package com.vardansoft.auth

object VardanSoftAuth {

    lateinit var GOOGLE_ID: String
    lateinit var AUTH_BASE_URL: String
    lateinit var RESOURCE_SERVERS: List<String>

    fun init(authBaseUrl: String, googleId: String, resourceServers: List<String>) {
        AUTH_BASE_URL = authBaseUrl.trimEnd('/')
        GOOGLE_ID = googleId
        RESOURCE_SERVERS = resourceServers
    }
}