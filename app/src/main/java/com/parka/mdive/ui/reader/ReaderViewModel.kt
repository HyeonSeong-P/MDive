package com.parka.mdive.ui.reader

import androidx.lifecycle.viewModelScope
import com.parka.mdive.domain.document.MarkdownDocument
import com.parka.mdive.domain.document.MarkdownParser
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

enum class ReaderContent { Loading, Empty, Failure, SourceReady }

data class ReaderState(
    val title: String? = null,
    val content: ReaderContent = ReaderContent.Loading,
    val document: SourceDocument? = null,
    val failure: DocumentFailure? = null,
    val markdown: MarkdownDocument? = null,
) : UiState

sealed interface ReaderIntent : UiIntent {
    data class Open(val reference: DocumentReference) : ReaderIntent
    data class CopyCode(val source: String) : ReaderIntent
    data object Back : ReaderIntent
    data object Retry : ReaderIntent
}

sealed interface ReaderEffect : UiEffect {
    data class CopyCode(val source: String) : ReaderEffect
    data object NavigateHome : ReaderEffect
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: DocumentRepository,
    private val parser: MarkdownParser,
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
            is ReaderIntent.CopyCode -> if (!leaving && uiState.value.content == ReaderContent.SourceReady) {
                viewModelScope.launch { emitEffect(ReaderEffect.CopyCode(intent.source)) }
            }
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
        reduce { copy(content = ReaderContent.Loading, document = null, failure = null, markdown = null) }
        readJob = viewModelScope.launch {
            val result = repository.read(selected)
            ensureActive()
            if (leaving) return@launch
            when (result) {
                is DocumentReadResult.Success -> {
                    val markdown = try {
                        parser.parse(result.document.source)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        reduce { copy(title = result.document.title, content = ReaderContent.Failure, failure = DocumentFailure.Read) }
                        return@launch
                    }
                    ensureActive()
                    if (leaving) return@launch
                    reduce { ReaderState(result.document.title,
                        if (result.document.source.isEmpty()) ReaderContent.Empty else ReaderContent.SourceReady,
                        result.document, markdown = markdown) }
                }
                is DocumentReadResult.Failure -> reduce { copy(content = ReaderContent.Failure, failure = result.reason) }
            }
        }
    }
}
