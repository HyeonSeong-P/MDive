package com.parka.mdive.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.parka.mdive.domain.document.DocumentReference
import com.parka.mdive.ui.base.BaseViewModel
import com.parka.mdive.ui.base.UiEffect
import com.parka.mdive.ui.base.UiIntent
import com.parka.mdive.ui.base.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class HomeState(val fileOpeningAvailable: Boolean = true, val selectionFailure: Boolean = false) : UiState

sealed interface HomeIntent : UiIntent {
    data object OpenFile : HomeIntent
    data class Selected(val reference: DocumentReference?) : HomeIntent
    data object PickerUnavailable : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data object SelectDocument : HomeEffect
    data class OpenDocument(val reference: DocumentReference) : HomeEffect
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
) : BaseViewModel<HomeState, HomeIntent, HomeEffect>(HomeState(fileOpeningAvailable = savedState.get<Boolean>("pickerPending") != true)) {
    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.OpenFile -> if (uiState.value.fileOpeningAvailable) {
                savedState["pickerPending"] = true
                reduce { copy(fileOpeningAvailable = false, selectionFailure = false) }
                viewModelScope.launch { emitEffect(HomeEffect.SelectDocument) }
            }
            is HomeIntent.Selected -> if (savedState.get<Boolean>("pickerPending") == true) {
                savedState["pickerPending"] = false
                reduce { copy(fileOpeningAvailable = true) }
                intent.reference?.let { viewModelScope.launch { emitEffect(HomeEffect.OpenDocument(it)) } }
            }
            HomeIntent.PickerUnavailable -> {
                savedState["pickerPending"] = false
                reduce { copy(fileOpeningAvailable = true, selectionFailure = true) }
            }
        }
    }
}
