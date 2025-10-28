package pitampoudel.core.presentation

sealed interface ResultUiEvent {
    data object Completed : ResultUiEvent
}