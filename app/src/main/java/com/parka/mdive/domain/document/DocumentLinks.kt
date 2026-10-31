package com.parka.mdive.domain.document

/** Platform-free results: a resolved locator still does not grant access outside its tree. */
enum class LinkFailure { Unsupported, BrowserUnavailable, Fragment, OutsideFolder, FolderRequired, Access, Missing, Read }
sealed interface DocumentLinkResult {
    data class Web(val destination: String) : DocumentLinkResult
    data class Ready(val reference: DocumentReference) : DocumentLinkResult
    data class Failure(val reason: LinkFailure) : DocumentLinkResult
}
interface DocumentLinkRepository {
    suspend fun resolve(document: DocumentReference, destination: String): DocumentLinkResult
    /** Validate the candidate before replacing the document's previous successful connection. */
    suspend fun connect(document: DocumentReference, destination: String, folder: String): DocumentLinkResult
}
