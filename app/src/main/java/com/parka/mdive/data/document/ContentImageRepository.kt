package com.parka.mdive.data.document

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.parka.mdive.domain.document.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import java.io.InterruptedIOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** All URI permissions, provider navigation, network, and decoder types stay inside data. */
@Singleton
class ContentImageRepository @Inject constructor(@ApplicationContext private val context: Context) : ImageRepository {
    internal val readStarts get() = decoder.fetchStarts
    internal val decodeStarts get() = decoder.decodeStarts
    private val resolver = context.contentResolver
    private val connections = context.getSharedPreferences("image-folders", Context.MODE_PRIVATE)
    @Suppress("DEPRECATION") // The Int overload is available on every supported API level.
    private val web = ImageWebFetcher(userAgent = "MDive/${context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty().ifBlank { "unknown" }} (https://github.com/HyeonSeong-P/MDive) okhttp/${okhttp3.OkHttp.VERSION}")
    private val decoder = ImageDecoder(context)

    override suspend fun connect(document: DocumentReference, folder: String): ImageFailure? = withContext(Dispatchers.IO) {
        try {
            val tree = Uri.parse(folder)
            if (!DocumentsContract.isTreeUri(tree)) return@withContext ImageFailure.Access
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            resolver.query(root, arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst() || cursor.getString(0) != DocumentsContract.Document.MIME_TYPE_DIR) throw ImageProblem(ImageFailure.Access)
            } ?: throw ImageProblem(ImageFailure.Access)
            ensureActive()
            resolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Preserve the previous successful mapping if persistence cannot complete.
            ensureActive()
            if (!connections.edit().putString(sha256(document.value), tree.toString()).putLong("generation:" + sha256(document.value), connections.getLong("generation:" + sha256(document.value), 0) + 1).commit()) return@withContext ImageFailure.Access
            null
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { ImageFailure.Access }
    }

    override suspend fun read(document: DocumentReference, destination: String, bypassCache: Boolean, documentVersion: String): ImageReadResult = withContext(Dispatchers.IO) {
        try {
            ensureActive()
            val isWeb = destination.startsWith("https:", true)
            val segments = if (isWeb) emptyList() else imageSegments(destination)
            val tree = if (isWeb) null else connections.getString(sha256(document.value), null)?.let(Uri::parse) ?: throw ImageProblem(ImageFailure.FolderRequired)
            val key = sha256("coil-static-v1|${document.value}|$documentVersion|${tree?.toString().orEmpty()}|${if (isWeb) 0 else connections.getLong("generation:" + sha256(document.value), 0)}|$destination")
            val raster = decoder.load(key, isWeb, bypassCache) {
                if (isWeb) web.fetch(destination) else {
                requireNotNull(tree)
                var id = DocumentsContract.getTreeDocumentId(tree)
                segments.forEachIndexed { index, segment ->
                    ensureActive()
                    val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
                    var found: Pair<String, String>? = null
                    resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { cursor ->
                        while (cursor.moveToNext()) {
                            if (cursor.getString(1) == segment) {
                                if (found != null) throw ImageProblem(ImageFailure.Missing) // Ambiguous provider names are not guessed.
                                found = cursor.getString(0) to cursor.getString(2)
                            }
                        }
                    } ?: throw ImageProblem(ImageFailure.Access)
                    val child = found ?: throw ImageProblem(ImageFailure.Missing)
                    if (index < segments.lastIndex && child.second != DocumentsContract.Document.MIME_TYPE_DIR) throw ImageProblem(ImageFailure.Missing)
                    if (index == segments.lastIndex && child.second == DocumentsContract.Document.MIME_TYPE_DIR) throw ImageProblem(ImageFailure.Missing)
                    id = child.first
                }
                val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                resolver.openInputStream(uri)?.use { input -> readImageBytes(input) { ensureActive() } } ?: throw ImageProblem(ImageFailure.Access)
                }
            }
            ensureActive()
            ImageReadResult.Ready(raster)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (e: ImageProblem) { ImageReadResult.Failure(e.reason) }
        catch (_: SecurityException) { ImageReadResult.Failure(ImageFailure.Access) }
        catch (_: FileNotFoundException) { ImageReadResult.Failure(ImageFailure.Missing) }
        catch (_: InterruptedIOException) { ImageReadResult.Failure(ImageFailure.Timeout) }
        catch (_: java.io.IOException) { ImageReadResult.Failure(ImageFailure.Network) }
        catch (_: StackOverflowError) { ImageReadResult.Failure(ImageFailure.Unsupported) }
        catch (_: OutOfMemoryError) { ImageReadResult.Failure(ImageFailure.TooLarge) }
        catch (_: Exception) { ImageReadResult.Failure(ImageFailure.Decode) }
    }
}
