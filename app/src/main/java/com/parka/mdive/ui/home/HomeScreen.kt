package com.parka.mdive.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parka.mdive.designsystem.MDiveBrandMark
import com.parka.mdive.designsystem.MDiveButton
import com.parka.mdive.designsystem.MDiveButtonSize
import com.parka.mdive.designsystem.MDiveButtonState
import com.parka.mdive.designsystem.MDiveDesignTheme
import com.parka.mdive.designsystem.MDiveWelcomeMark
import com.parka.mdive.designsystem.MDiveStateActionWidth
import com.parka.mdive.designsystem.MDiveStateKind
import com.parka.mdive.designsystem.MDiveStatePanel
import com.parka.mdive.designsystem.MDiveTopBar
import com.parka.mdive.ui.base.CollectEffects
import com.parka.mdive.ui.base.CenteredScrollableState

@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { /* No home effects exist in S01. */ }
    HomeScreen(state = state, onOpenFile = { viewModel.onIntent(HomeIntent.OpenFile) })
}

@Composable
fun HomeScreen(state: HomeState, onOpenFile: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            MDiveTopBar(leading = { MDiveBrandMark() })
            CenteredScrollableState(Modifier.weight(1f)) {
                MDiveStatePanel(
                    title = "반가워요.",
                    detail = if (state.fileOpeningAvailable) "Markdown 문서를 열고 편안하게 읽어보세요."
                    else "Markdown 문서를 열고 편안하게 읽어보세요.\n아직 파일 열기는 사용할 수 없어요.",
                    kind = MDiveStateKind.Empty,
                    showSymbol = false,
                    actionWidth = MDiveStateActionWidth.Compact,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    decoration = { MDiveWelcomeMark(Modifier.testTag("welcome-illustration")) },
                    decorationSpacing = 28.dp,
                    titleSpacing = 8.dp,
                    actions = {
                        MDiveButton(
                            text = "파일 열기",
                            onClick = onOpenFile,
                            size = MDiveButtonSize.L,
                            state = if (state.fileOpeningAvailable) MDiveButtonState.Normal else MDiveButtonState.Disabled,
                        )
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() = MDiveDesignTheme { HomeScreen(HomeState(), {}) }
