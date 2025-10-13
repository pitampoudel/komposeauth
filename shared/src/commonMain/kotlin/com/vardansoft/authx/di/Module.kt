package com.vardansoft.authx.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import com.vardansoft.authx.data.AuthXClientImpl
import com.vardansoft.authx.data.AuthXImpl
import com.vardansoft.authx.data.AuthXPreferencesImpl
import com.vardansoft.authx.domain.AuthX
import com.vardansoft.authx.domain.AuthXClient
import com.vardansoft.authx.domain.AuthXPreferences
import org.koin.dsl.module

fun authXSharedModule(authUrl: String, serverUrls: List<String>) = module {
    single<ObservableSettings> {
        Settings().makeObservable()
    }
    single<AuthXPreferences> {
        AuthXPreferencesImpl(get())
    }
    single<AuthX> {
        AuthXImpl(
            authXPreferences = get<AuthXPreferences>(),
            authUrl = authUrl,
            serverUrls = serverUrls
        )
    }
    single<AuthXClient> {
        AuthXClientImpl(get(), authUrl)
    }
}