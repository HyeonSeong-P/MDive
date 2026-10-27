package com.parka.mdive.ui.reader

import androidx.lifecycle.viewModelScope
import com.parka.mdive.domain.document.DocumentFailure
import com.parka.mdive.domain.document.DocumentReadResult
import com.parka.mdive.domain.document.DocumentReference
import com.parka.mdive.domain.document.DocumentRepository
import com.parka.mdive.domain.document.SourceDocument
import com.parka.mdive.ui.base.BaseViewModel
import com.parka.mdive.ui.base.UiEffect
import com.parka.mdive.ui.base.UiIntent
import com.parka.mdive.ui.base.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

enum class ReaderContent { Loading, Empty, Failure, SourceReady }

data class ReaderState(
    val title: String? = null,
    val content: ReaderContent = ReaderContent.Loading,
    val document: SourceDocument? = null,
    val failure: DocumentFailure? = null,
) : UiState

sealed interface ReaderIntent : UiIntent {
    data class Open(val reference: DocumentReference) : ReaderIntent
    data object Back : ReaderIntent
    data object Retry : ReaderIntent
}

sealed interface ReaderEffect : UiEffect {
    data object NavigateHome : ReaderEffect
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: DocumentRepository,
) : BaseViewModel<ReaderState, ReaderIntent, ReaderEffect>(ReaderState()) {
    private var reference: DocumentReference? = null
    private var readJob: Job? = null
    private var leaving = false

    override fun onIntent(intent: ReaderIntent) {
        when (intent) {
            is ReaderIntent.Open -> if (reference == null && !leaving) {
                reference = intent.reference
                read()
            }
            ReaderIntent.Retry -> if (!leaving && uiState.value.content == ReaderContent.Failure) read()
            ReaderIntent.Back -> {
                leaving = true
                readJob?.cancel()
                viewModelScope.launch { emitEffect(ReaderEffect.NavigateHome) }
            }
        }
    }

    private fun read() {
        val selected = reference ?: return
        if (readJob?.isActive == true) return
        reduce { copy(content = ReaderContent.Loading, document = null, failure = null) }
        readJob = viewModelScope.launch {
            val result = repository.read(selected)
            ensureActive()
            if (leaving) return@launch
            when (result) {
                is DocumentReadResult.Success -> reduce {
                    ReaderState(result.document.title,
                        if (result.document.source.isEmpty()) ReaderContent.Empty else ReaderContent.SourceReady,
                        result.document)
                }
                is DocumentReadResult.Failure -> reduce { copy(content = ReaderContent.Failure, failure = result.reason) }
            }
        }
    }
}
