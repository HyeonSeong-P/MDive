package com.parka.mdive.testing

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import com.parka.mdive.ui.reader.ReaderScreen
import com.parka.mdive.ui.reader.ReaderState

/** The temporary raw-source presentation exists only in debug development verification. */
@Composable
fun SourceDocumentTestHost(state: ReaderState, onBack: () -> Unit, onRetry: () -> Unit) {
    ReaderScreen(state, onBack, onRetry, sourcePreview = { document -> BasicText(document.source) })
}
