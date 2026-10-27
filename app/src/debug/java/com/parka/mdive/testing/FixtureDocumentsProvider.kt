package com.parka.mdive.testing

import android.database.Cursor
import android.database.MatrixCursor
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import java.io.File
import java.io.FileNotFoundException

/** Debug-only deterministic SAF source for loader and real system-picker verification. */
class FixtureDocumentsProvider : DocumentsProvider() {
    companion object {
        const val AUTHORITY = "com.parka.mdive.s02.fixtures"
        const val SOURCE = "# 실제 선택\r\n  원문 공백\t\n"
        @Volatile var failReads = false
    }
    override fun onCreate() = true
    override fun queryRoots(projection: Array<out String>?): Cursor = MatrixCursor(projection ?: arrayOf(
        Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_TITLE, Root.COLUMN_FLAGS, Root.COLUMN_MIME_TYPES,
    )).apply {
        newRow().add(Root.COLUMN_ROOT_ID, "s02").add(Root.COLUMN_DOCUMENT_ID, "root")
            .add(Root.COLUMN_TITLE, "MDive S02 tests").add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_IS_CHILD)
            .add(Root.COLUMN_MIME_TYPES, "*/*")
    }
    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor = cursor(projection).apply { addDocument(documentId) }
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor = cursor(projection).apply {
        listOf("source.md", "empty.txt", "utf16.markdown", "broken.md", "large.md", "unsupported.pdf", "missing.md", "nameless").forEach { addDocument(it) }
    }
    override fun isChildDocument(parentDocumentId: String, documentId: String) = parentDocumentId == "root" && documentId != "root"
    private fun cursor(projection: Array<out String>?) = MatrixCursor(projection ?: arrayOf(
        Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_FLAGS, Document.COLUMN_SIZE,
    ))
    private fun MatrixCursor.addDocument(id: String) {
        newRow().add(Document.COLUMN_DOCUMENT_ID, id).add(Document.COLUMN_DISPLAY_NAME, if (id == "nameless") null else id)
            .add(Document.COLUMN_MIME_TYPE, if (id == "root") Document.MIME_TYPE_DIR else "application/octet-stream")
            .add(Document.COLUMN_FLAGS, 0).add(Document.COLUMN_SIZE, null)
    }
    override fun openDocument(documentId: String, mode: String, signal: android.os.CancellationSignal?): ParcelFileDescriptor {
        if (failReads || documentId == "missing.md") throw FileNotFoundException("Fixture unavailable")
        val bytes = when (documentId) {
            "source.md" -> SOURCE.toByteArray()
            "empty.txt" -> byteArrayOf()
            "utf16.markdown" -> byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + SOURCE.toByteArray(Charsets.UTF_16LE)
            "broken.md" -> byteArrayOf(0xC3.toByte(), 0x28)
            "large.md" -> ByteArray(5 * 1024 * 1024 + 1) { 65 }
            else -> "fixture".toByteArray()
        }
        val file = File(requireNotNull(context).cacheDir, "s02-$documentId")
        file.writeBytes(bytes)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
}
