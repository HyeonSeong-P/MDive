package com.parka.mdive.ui.home

import com.parka.mdive.ui.base.BaseViewModel
import com.parka.mdive.ui.base.UiEffect
import com.parka.mdive.ui.base.UiIntent
import com.parka.mdive.ui.base.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** File selection is deliberately unavailable until S02 connects the system picker. */
data class HomeState(val fileOpeningAvailable: Boolean = false) : UiState

sealed interface HomeIntent : UiIntent {
    data object OpenFile : HomeIntent
}

sealed interface HomeEffect : UiEffect

@HiltViewModel
class HomeViewModel @Inject constructor() : BaseViewModel<HomeState, HomeIntent, HomeEffect>(HomeState()) {
    override fun onIntent(intent: HomeIntent) {
        // S01 exposes no file-selection side effect or prepared document in the product.
        when (intent) {
            HomeIntent.OpenFile -> Unit
        }
    }
}
