package com.parka.mdive.domain.document

/** An opaque locator supplied by the platform boundary; it grants no surrounding-file access. */
data class DocumentReference(val value: String)

data class SourceDocument(val title: String, val source: String)

enum class DocumentFailure { Access, UnsupportedFile, TooLarge, Encoding, Read }

sealed interface DocumentReadResult {
    data class Success(val document: SourceDocument) : DocumentReadResult
    data class Failure(val reason: DocumentFailure) : DocumentReadResult
}

interface DocumentRepository {
    suspend fun read(reference: DocumentReference): DocumentReadResult
}
