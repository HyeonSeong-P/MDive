package com.parka.mdive.ui

import com.parka.mdive.ui.home.HomeIntent
import com.parka.mdive.ui.home.HomeState
import com.parka.mdive.ui.home.HomeViewModel
import com.parka.mdive.ui.reader.ReaderEffect
import com.parka.mdive.ui.reader.ReaderIntent
import com.parka.mdive.ui.reader.ReaderState
import com.parka.mdive.ui.reader.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenViewModelTest {
    @Test
    fun homeDoesNotPretendToSelectOrOpenADocument() {
        val viewModel = HomeViewModel()
        viewModel.onIntent(HomeIntent.OpenFile)
        assertEquals(HomeState(fileOpeningAvailable = false), viewModel.uiState.value)
    }

    @Test
    fun readerRequestsHostActionsWithoutLoadingPreparedContent() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel = ReaderViewModel()
            listOf(
                ReaderIntent.Back to ReaderEffect.NavigateHome,
                ReaderIntent.Retry to ReaderEffect.RetryRequested,
            ).forEach { (intent, effect) ->
                viewModel.onIntent(intent)
                assertEquals(effect, viewModel.effects.first())
                assertEquals(ReaderState(), viewModel.uiState.value)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
