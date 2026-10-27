package com.parka.mdive.ui

import com.parka.mdive.domain.document.*
import com.parka.mdive.ui.home.*
import com.parka.mdive.ui.reader.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenViewModelTest {
    @Test fun homeLaunchesOnceAndCancellationLeavesHomeAvailable() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val vm = HomeViewModel(androidx.lifecycle.SavedStateHandle())
            vm.onIntent(HomeIntent.OpenFile)
            vm.onIntent(HomeIntent.OpenFile)
            assertFalse(vm.uiState.value.fileOpeningAvailable)
            assertEquals(HomeEffect.SelectDocument, vm.effects.first())
            vm.onIntent(HomeIntent.Selected(null))
            assertEquals(HomeState(), vm.uiState.value)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun pendingPickerRestoresWithoutLaunchingAgainAndAcceptsResult() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val saved = androidx.lifecycle.SavedStateHandle(mapOf("pickerPending" to true))
            val vm = HomeViewModel(saved)
            assertFalse(vm.uiState.value.fileOpeningAvailable)
            vm.onIntent(HomeIntent.OpenFile)
            val reference = DocumentReference("selected")
            vm.onIntent(HomeIntent.Selected(reference))
            assertEquals(HomeEffect.OpenDocument(reference), vm.effects.first())
            assertTrue(vm.uiState.value.fileOpeningAvailable)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun readerRetriesSameReferenceAndDistinguishesEmptyFromWhitespace() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val references = mutableListOf<DocumentReference>()
            val results = ArrayDeque(listOf(
                DocumentReadResult.Failure(DocumentFailure.Access),
                DocumentReadResult.Success(SourceDocument("file.md", " \n")),
            ))
            val vm = ReaderViewModel(object : DocumentRepository {
                override suspend fun read(reference: DocumentReference): DocumentReadResult {
                    references += reference
                    return results.removeFirst()
                }
            })
            val ref = DocumentReference("opaque")
            vm.onIntent(ReaderIntent.Open(ref))
            assertEquals(DocumentFailure.Access, vm.uiState.value.failure)
            vm.onIntent(ReaderIntent.Retry)
            assertEquals(listOf(ref, ref), references)
            assertEquals(ReaderContent.SourceReady, vm.uiState.value.content)
            assertEquals(" \n", vm.uiState.value.document?.source)
            val emptyVm = ReaderViewModel(object : DocumentRepository {
                override suspend fun read(reference: DocumentReference) = DocumentReadResult.Success(SourceDocument("empty.txt", ""))
            })
            emptyVm.onIntent(ReaderIntent.Open(ref))
            assertEquals(ReaderContent.Empty, emptyVm.uiState.value.content)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun pendingReadIgnoresDuplicateAndBackCancelsWithoutLateContent() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val result = CompletableDeferred<DocumentReadResult>()
            var calls = 0
            val vm = ReaderViewModel(object : DocumentRepository {
                override suspend fun read(reference: DocumentReference): DocumentReadResult { calls++; return result.await() }
            })
            vm.onIntent(ReaderIntent.Open(DocumentReference("one")))
            vm.onIntent(ReaderIntent.Open(DocumentReference("two")))
            vm.onIntent(ReaderIntent.Retry)
            assertEquals(1, calls)
            assertEquals(ReaderContent.Loading, vm.uiState.value.content)
            vm.onIntent(ReaderIntent.Back)
            assertEquals(ReaderEffect.NavigateHome, vm.effects.first())
            result.complete(DocumentReadResult.Success(SourceDocument("late.md", "late")))
            assertNull(vm.uiState.value.document)
        } finally { Dispatchers.resetMain() }
    }
}
