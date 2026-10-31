package com.parka.mdive.data.document

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.parka.mdive.domain.document.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

@Singleton
class ContentDocumentLinkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documents: ContentDocumentRepository,
) : DocumentLinkRepository {
    private val resolver = context.contentResolver
    private val connections = context.getSharedPreferences("image-folders", Context.MODE_PRIVATE)

    override suspend fun resolve(document: DocumentReference, destination: String): DocumentLinkResult = withContext(Dispatchers.IO) {
        guarded {
            if (destination.startsWith("http:", true) || destination.startsWith("https:", true)) {
                if (checkedWebLink(destination)) return@guarded DocumentLinkResult.Web(destination)
                throw LinkProblem(LinkFailure.Unsupported)
            }
            // Validate even when no tree has been selected. No unsupported URI opens a picker.
            val key = sha256(document.value)
            val connection = connections.all
            val directory = (connection["directory:$key"] as? String)?.split('/')?.filter { it.isNotEmpty() }.orEmpty()
            val path = linkSegments(destination, directory)
            val tree = (connection[key] as? String)?.let(Uri::parse) ?: throw LinkProblem(LinkFailure.FolderRequired)
            locate(tree, path)
        }
    }

    override suspend fun connect(document: DocumentReference, destination: String, folder: String): DocumentLinkResult = withContext(Dispatchers.IO) {
        guarded {
            val tree = Uri.parse(folder)
            if (!DocumentsContract.isTreeUri(tree)) throw LinkProblem(LinkFailure.Access)
            // A newly chosen folder is explicitly the current document's directory.
            val path = linkSegments(destination)
            val ready = locate(tree, path, persistTarget = false)
            // A wrong folder or unreadable target never replaces an existing successful mapping.
            when (documents.read(ready.reference)) {
                is DocumentReadResult.Failure -> throw LinkProblem(LinkFailure.Read)
                else -> Unit
            }
            ensureActive()
            resolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ensureActive()
            if (!connections.edit().putString(sha256(document.value), tree.toString())
                    .remove("directory:" + sha256(document.value))
                    .putLong("generation:" + sha256(document.value), connections.getLong("generation:" + sha256(document.value), 0) + 1)
                    .putString(sha256(ready.reference.value), tree.toString())
                    .putString("directory:" + sha256(ready.reference.value), path.dropLast(1).joinToString("/"))
                    .putLong("generation:" + sha256(ready.reference.value), connections.getLong("generation:" + sha256(ready.reference.value), 0) + 1)
                    .commit()) throw LinkProblem(LinkFailure.Access)
            ready
        }
    }

    private suspend fun locate(tree: Uri, path: List<String>, persistTarget: Boolean = true): DocumentLinkResult.Ready {
        if (!DocumentsContract.isTreeUri(tree)) throw LinkProblem(LinkFailure.Access)
        var id = DocumentsContract.getTreeDocumentId(tree)
        path.forEachIndexed { index, segment ->
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            var found: Pair<String, String>? = null
            resolver.query(DocumentsContract.buildChildDocumentsUriUsingTree(tree, id), arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) if (cursor.getString(1) == segment) {
                    if (found != null) throw LinkProblem(LinkFailure.Missing)
                    found = cursor.getString(0) to cursor.getString(2)
                }
            } ?: throw LinkProblem(LinkFailure.Access)
            val child = found ?: throw LinkProblem(LinkFailure.Missing)
            if ((index < path.lastIndex) != (child.second == DocumentsContract.Document.MIME_TYPE_DIR)) throw LinkProblem(LinkFailure.Missing)
            id = child.first
        }
        val reference = DocumentReference(DocumentsContract.buildDocumentUriUsingTree(tree, id).toString())
        if (persistTarget) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            if (!connections.edit().putString(sha256(reference.value), tree.toString()).putString("directory:" + sha256(reference.value), path.dropLast(1).joinToString("/")).commit()) throw LinkProblem(LinkFailure.Access)
        }
        return DocumentLinkResult.Ready(reference)
    }

    private suspend fun guarded(block: suspend () -> DocumentLinkResult): DocumentLinkResult = try { block() }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (problem: LinkProblem) { DocumentLinkResult.Failure(problem.reason) }
    catch (_: SecurityException) { DocumentLinkResult.Failure(LinkFailure.Access) }
    catch (_: java.io.FileNotFoundException) { DocumentLinkResult.Failure(LinkFailure.Missing) }
    catch (_: Exception) { DocumentLinkResult.Failure(LinkFailure.Read) }
}
