package com.vardansoft.auth

import com.vardansoft.auth.di.loginModule
import org.koin.core.module.Module

object VardanSoftAuth {

    fun getAllModules(): List<Module> {
        return listOf(loginModule)
    }

    val GOOGLE_ID = BuildKonfig.AUTH_GOOGLE_ID
    internal val URL = BuildKonfig.AUTH_URL

    object EndPoints {
        fun apiUrl(endPoint: String): String {
            return "${URL}/$endPoint"
        }

        const val TOKEN = "oauth2/token"
        const val USER_INFO = "userinfo"
    }

}