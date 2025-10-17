package com.vardansoft.komposeauth.ui.core.presentation

sealed interface ResultUiEvent {
    data object Completed : ResultUiEvent
}