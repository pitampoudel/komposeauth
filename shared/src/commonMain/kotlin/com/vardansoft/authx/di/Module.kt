package com.vardansoft.authx.di

import com.vardansoft.authx.data.AuthXClientImpl
import com.vardansoft.authx.data.AuthXImpl
import com.vardansoft.authx.data.AuthXPreferencesImpl
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.authx.domain.AuthX
import com.vardansoft.authx.domain.AuthXPreferences
import com.vardansoft.authx.domain.use_cases.ValidateConfirmPassword
import com.vardansoft.authx.domain.use_cases.ValidateEmail
import com.vardansoft.authx.domain.use_cases.ValidateOtpCode
import com.vardansoft.authx.domain.use_cases.ValidatePassword
import com.vardansoft.authx.domain.use_cases.ValidatePhoneNumber
import com.vardansoft.authx.domain.use_cases.ValidateNotBlank
import com.vardansoft.authx.domain.use_cases.ValidateNotNull
import org.koin.dsl.module

fun authXSharedModule(authUrl: String, clientId: String, serverUrls: List<String>) = module {
    single<AuthXPreferences> {
        AuthXPreferencesImpl(get(), get())
    }
    single<ValidateOtpCode> {
        ValidateOtpCode()
    }
    single<ValidatePassword> {
        ValidatePassword()
    }
    single<ValidateConfirmPassword> {
        ValidateConfirmPassword()
    }
    single<ValidatePhoneNumber> {
        ValidatePhoneNumber()
    }
    single<ValidateEmail> {
        ValidateEmail()
    }
    single<ValidateNotBlank> {
        ValidateNotBlank()
    }
    single<ValidateNotNull> {
        ValidateNotNull()
    }
    single<AuthX> {
        AuthXImpl(
            authXPreferences = get<AuthXPreferences>(),
            authUrl = authUrl,
            clientId = clientId,
            serverUrls = serverUrls
        )
    }
    single<AuthXClient> {
        AuthXClientImpl(get(), authUrl)
    }
}