package com.parka.mdive.catalog.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = mutableUiState.asStateFlow()

    private val effectChannel = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = effectChannel.receiveAsFlow()

    abstract fun onIntent(intent: I)

    /** 현재 상태를 기반으로 다음 상태를 원자적으로 계산한다. */
    protected fun reduce(transform: S.() -> S) {
        mutableUiState.update { current -> current.transform() }
    }

    /** 화면의 effect collector가 소비할 때까지 채널에 보관한다. */
    protected suspend fun emitEffect(effect: E) {
        effectChannel.send(effect)
    }
}
