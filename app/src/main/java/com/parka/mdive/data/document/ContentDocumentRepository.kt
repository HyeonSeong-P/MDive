package com.parka.mdive.data.document

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.parka.mdive.domain.document.DocumentFailure
import com.parka.mdive.domain.document.DocumentReadResult
import com.parka.mdive.domain.document.DocumentReference
import com.parka.mdive.domain.document.DocumentRepository
import com.parka.mdive.domain.document.SourceDocument
import java.io.FileNotFoundException
import java.nio.charset.CharacterCodingException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class ContentDocumentRepository @Inject constructor(
    private val resolver: ContentResolver,
) : DocumentRepository {
    override suspend fun read(reference: DocumentReference): DocumentReadResult = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(reference.value)
            if (uri.scheme != ContentResolver.SCHEME_CONTENT) return@withContext DocumentReadResult.Failure(DocumentFailure.Access)
            val title = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { cursor.getString(it) } else null
            }
            if (title.isNullOrBlank() || title.substringAfterLast('.', "").lowercase() !in setOf("md", "markdown", "txt")) {
                return@withContext DocumentReadResult.Failure(DocumentFailure.UnsupportedFile)
            }
            coroutineContext.ensureActive()
            val bytes = resolver.openInputStream(uri)?.use { readBoundedSource(it) }
                ?: return@withContext DocumentReadResult.Failure(DocumentFailure.Read)
            coroutineContext.ensureActive()
            val source = decodeSource(bytes)
            coroutineContext.ensureActive()
            DocumentReadResult.Success(SourceDocument(title, source))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            DocumentReadResult.Failure(DocumentFailure.Access)
        } catch (_: FileNotFoundException) {
            DocumentReadResult.Failure(DocumentFailure.Access)
        } catch (_: DocumentTooLargeException) {
            DocumentReadResult.Failure(DocumentFailure.TooLarge)
        } catch (_: CharacterCodingException) {
            DocumentReadResult.Failure(DocumentFailure.Encoding)
        } catch (_: Exception) {
            DocumentReadResult.Failure(DocumentFailure.Read)
        }
    }
}
