package com.vardansoft.authx.ui.core

sealed interface ResultUiEvent {
    data object Completed : ResultUiEvent
}