package com.vardansoft.auth.domain

sealed class LazyState<out T> {
    object Loading : LazyState<Nothing>()
    data class Loaded<T>(val value: T?) : LazyState<T>()

    val isLoading: Boolean get() = this is Loading
    val valueOrNull: T? get() = (this as? Loaded)?.value
    val valueOrThrow: T get() = (this as Loaded).value!!


}