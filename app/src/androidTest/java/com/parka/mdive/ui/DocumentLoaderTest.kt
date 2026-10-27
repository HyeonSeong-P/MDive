package com.parka.mdive.ui

import android.content.Context
import android.provider.DocumentsContract
import androidx.test.platform.app.InstrumentationRegistry
import com.parka.mdive.data.document.ContentDocumentRepository
import com.parka.mdive.domain.document.*
import com.parka.mdive.testing.FixtureDocumentsProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DocumentLoaderTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun reference(id: String) = DocumentReference(DocumentsContract.buildDocumentUri(FixtureDocumentsProvider.AUTHORITY, id).toString())

    @Test fun providerSourceTitleEncodingEmptyAndUnknownSizeAreRead() = runBlocking {
        val repository = ContentDocumentRepository(context.contentResolver)
        for (id in listOf("source.md", "utf16.markdown")) {
            val result = repository.read(reference(id)) as DocumentReadResult.Success
            assertEquals(id, result.document.title)
            assertEquals(FixtureDocumentsProvider.SOURCE, result.document.source)
        }
        assertEquals("", (repository.read(reference("empty.txt")) as DocumentReadResult.Success).document.source)
    }

    @Test fun providerFailureBoundariesAndRetryReadAgain() = runBlocking {
        val repository = ContentDocumentRepository(context.contentResolver)
        for ((id, expected) in mapOf(
            "broken.md" to DocumentFailure.Encoding,
            "large.md" to DocumentFailure.TooLarge,
            "unsupported.pdf" to DocumentFailure.UnsupportedFile,
            "nameless" to DocumentFailure.UnsupportedFile,
            "missing.md" to DocumentFailure.Access,
        )) assertEquals(id, DocumentReadResult.Failure(expected), repository.read(reference(id)))
        assertEquals(DocumentReadResult.Failure(DocumentFailure.Access), repository.read(DocumentReference("file:///not-allowed.md")))
        try {
            FixtureDocumentsProvider.failReads = true
            assertEquals(DocumentReadResult.Failure(DocumentFailure.Access), repository.read(reference("source.md")))
        } finally { FixtureDocumentsProvider.failReads = false }
        assertTrue(repository.read(reference("source.md")) is DocumentReadResult.Success)
    }
}
