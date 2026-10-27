package com.parka.mdive.ui.reader

import androidx.lifecycle.viewModelScope
import com.parka.mdive.ui.base.BaseViewModel
import com.parka.mdive.ui.base.UiEffect
import com.parka.mdive.ui.base.UiIntent
import com.parka.mdive.ui.base.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

enum class ReaderContent { Loading, Empty, Failure }

data class ReaderState(
    val title: String? = null,
    val content: ReaderContent = ReaderContent.Loading,
) : UiState

sealed interface ReaderIntent : UiIntent {
    data object Back : ReaderIntent
    data object Retry : ReaderIntent
}

sealed interface ReaderEffect : UiEffect {
    data object NavigateHome : ReaderEffect
    data object RetryRequested : ReaderEffect
}

@HiltViewModel
class ReaderViewModel @Inject constructor() : BaseViewModel<ReaderState, ReaderIntent, ReaderEffect>(ReaderState()) {
    override fun onIntent(intent: ReaderIntent) {
        viewModelScope.launch {
            emitEffect(when (intent) {
                ReaderIntent.Back -> ReaderEffect.NavigateHome
                ReaderIntent.Retry -> ReaderEffect.RetryRequested
            })
        }
    }
}
