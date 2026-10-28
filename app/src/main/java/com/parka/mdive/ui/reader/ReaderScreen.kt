package com.parka.mdive.ui.reader

import android.content.ClipData
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import com.parka.mdive.designsystem.MDiveToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import com.parka.mdive.domain.document.DocumentReference
import com.parka.mdive.domain.document.DocumentFailure
import com.parka.mdive.domain.document.SourceDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parka.mdive.designsystem.MDiveButton
import com.parka.mdive.designsystem.MDiveButtonVariant
import com.parka.mdive.designsystem.MDiveDesignTheme
import com.parka.mdive.designsystem.MDiveStateActionWidth
import com.parka.mdive.designsystem.MDiveStateKind
import com.parka.mdive.designsystem.MDiveStatePanel
import com.parka.mdive.designsystem.MDiveTopBar
import com.parka.mdive.ui.base.CenteredScrollableState
import com.parka.mdive.ui.base.CollectEffects

@Composable
fun ReaderRoute(
    onNavigateHome: () -> Unit,
    reference: DocumentReference,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var feedback by remember { mutableStateOf<Pair<Long, String>?>(null) }
    LaunchedEffect(feedback) { if (feedback != null) { delay(3000); feedback = null } }
    LaunchedEffect(reference) { viewModel.onIntent(ReaderIntent.Open(reference)) }
    BackHandler { viewModel.onIntent(ReaderIntent.Back) }
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            ReaderEffect.NavigateHome -> onNavigateHome()
            is ReaderEffect.CopyCode -> scope.launch {
                val message = copyCodeWithFeedback(Build.VERSION.SDK_INT, effect.source) {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("코드", it)))
                }
                feedback = message?.let { System.nanoTime() to it }
            }
        }
    }
    Box {
        ReaderScreen(
            state = state,
            onBack = { viewModel.onIntent(ReaderIntent.Back) },
            onRetry = { viewModel.onIntent(ReaderIntent.Retry) },
            onCopyCode = { viewModel.onIntent(ReaderIntent.CopyCode(it)) },
        )
        feedback?.let { (_, message) -> MDiveToast(message, Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(16.dp),
            icon = if (message == COPY_FAILED) "warning" else "check") }
    }
}

@Composable
fun ReaderScreen(
    state: ReaderState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    sourcePreview: (@Composable (SourceDocument) -> Unit)? = null,
    onCopyCode: (String) -> Unit = {},
) {
    Surface(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            MDiveTopBar(
                title = state.title ?: "문서",
                navigationIcon = "back",
                navigationDescription = "홈으로 돌아가기",
                onNavigationClick = onBack,
            )
            if (state.content == ReaderContent.Loading) ReaderLoadingContent(Modifier.weight(1f))
            else if (state.content == ReaderContent.SourceReady && sourcePreview == null) {
                state.markdown?.let { ReaderMarkdownContent(it, Modifier.weight(1f), onCopyCode) }
            } else CenteredScrollableState(Modifier.weight(1f)) {
                when (state.content) {
                    ReaderContent.Loading -> Unit
                    ReaderContent.SourceReady -> {
                        val document = state.document
                        if (sourcePreview != null && document != null) sourcePreview(document)

                    }
                    ReaderContent.Empty -> MDiveStatePanel(
                        title = "빈 문서예요",
                        detail = "표시할 내용이 없습니다.",
                        kind = MDiveStateKind.Empty,
                        showSymbol = false,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        titleSpacing = 8.dp,
                    )
                    ReaderContent.Failure -> MDiveStatePanel(
                        title = "문서를 열지 못했어요",
                        detail = when (state.failure) {
                            DocumentFailure.Access -> "파일에 접근할 수 없어요. 접근 권한이나 파일 상태를 확인해주세요."
                            DocumentFailure.UnsupportedFile -> "파일 이름과 확장자를 확인해주세요. md, markdown, txt 파일을 지원해요."
                            DocumentFailure.TooLarge -> "5 MiB 이하의 파일을 열어주세요."
                            DocumentFailure.Encoding -> "UTF-8 또는 BOM이 있는 UTF-16 파일을 열어주세요."
                            else -> "문서를 읽을 수 없어요. 다시 시도하거나 홈으로 돌아가세요."
                        },
                        kind = MDiveStateKind.Failure,
                        showSymbol = false,
                        actionWidth = MDiveStateActionWidth.Compact,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        titleSpacing = 8.dp,
                        actions = {
                            MDiveButton("다시 시도", onRetry)
                            MDiveButton("홈으로", onBack, variant = MDiveButtonVariant.Quiet)
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReaderLoadingPreview() = MDiveDesignTheme { ReaderScreen(ReaderState(title = "예시.md"), {}, {}) }

@Preview(showBackground = true)
@Composable
private fun ReaderEmptyPreview() = MDiveDesignTheme { ReaderScreen(ReaderState("예시.md", ReaderContent.Empty), {}, {}) }

@Preview(showBackground = true)
@Composable
private fun ReaderFailurePreview() = MDiveDesignTheme { ReaderScreen(ReaderState("예시.md", ReaderContent.Failure), {}, {}) }

internal const val COPY_FAILED = "복사하지 못했어요. 다시 시도해주세요"
internal const val COPY_SUCCEEDED = "코드를 복사했어요"

/** Completion means the platform write returned; do not read the clipboard back. */
internal suspend fun copyCodeWithFeedback(apiLevel: Int, source: String, write: suspend (String) -> Unit): String? = try {
    write(source)
    if (apiLevel < 33) COPY_SUCCEEDED else null
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    COPY_FAILED
}
