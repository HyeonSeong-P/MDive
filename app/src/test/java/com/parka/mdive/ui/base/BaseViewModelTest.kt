package com.parka.mdive.ui.base

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BaseViewModelTest {

    @Test
    fun intentReducesState() {
        val viewModel = CounterViewModel()

        viewModel.onIntent(CounterIntent.Increment)
        viewModel.onIntent(CounterIntent.Increment)

        assertEquals(CounterState(count = 2), viewModel.uiState.value)
    }

    @Test
    fun bufferedEffectReachesCollectorStartedLater() = runBlocking {
        val viewModel = CounterViewModel()

        viewModel.sendNotice()

        assertEquals(CounterEffect.Notice, viewModel.effects.first())
    }

    private data class CounterState(val count: Int = 0) : UiState

    private sealed interface CounterIntent : UiIntent {
        data object Increment : CounterIntent
    }

    private sealed interface CounterEffect : UiEffect {
        data object Notice : CounterEffect
    }

    private class CounterViewModel : BaseViewModel<CounterState, CounterIntent, CounterEffect>(
        CounterState(),
    ) {
        override fun onIntent(intent: CounterIntent) {
            when (intent) {
                CounterIntent.Increment -> reduce { copy(count = count + 1) }
            }
        }

        suspend fun sendNotice() = emitEffect(CounterEffect.Notice)
    }
}
