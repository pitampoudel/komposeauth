package com.vardansoft.core.presentation

sealed interface ResultUiEvent {
    data object Completed : ResultUiEvent
}