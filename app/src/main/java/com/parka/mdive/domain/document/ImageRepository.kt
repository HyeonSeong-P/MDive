package com.parka.mdive.domain.document

/** Inert image reference; no URI, decoder, or UI types cross this contract. */
data class MarkdownImage(val destination: String, val alt: String, val title: String? = null)

enum class ImageFailure { FolderRequired, Access, Missing, OutsideFolder, Unsupported, TooLarge, Timeout, Network, Decode }

/** ARGB pixels in row order, owned by this result and never mutated after publication. */
class ImageRaster(val width: Int, val height: Int, val pixels: IntArray) {
    val byteCount: Long get() = pixels.size.toLong() * 4
}
sealed interface ImageReadResult {
    data class Ready(val raster: ImageRaster) : ImageReadResult
    data class Failure(val reason: ImageFailure) : ImageReadResult
}
interface ImageRepository {
    suspend fun read(document: DocumentReference, destination: String, bypassCache: Boolean = false, documentVersion: String = ""): ImageReadResult
    /** null means the new, persisted connection is ready; failures retain the previous mapping. */
    suspend fun connect(document: DocumentReference, folder: String): ImageFailure?
}
