package com.parka.mdive.data.document

import com.parka.mdive.domain.document.ImageFailure
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder

internal const val IMAGE_BYTE_LIMIT = 20 * 1024 * 1024
internal const val IMAGE_PIXEL_LIMIT = 16_000_000L
internal class ImageProblem(val reason: ImageFailure) : Exception()

/** Decode once, normalize segments, and never interpret provider document IDs as file paths. */
internal fun imageSegments(destination: String): List<String> {
    if (destination.startsWith("/") || destination.startsWith("\\") || destination.contains('?') || destination.contains('#') || Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(destination)) throw ImageProblem(ImageFailure.OutsideFolder)
    val decoded = try { URLDecoder.decode(destination.replace("+", "%2B"), "UTF-8") } catch (_: IllegalArgumentException) { throw ImageProblem(ImageFailure.OutsideFolder) }
    if (decoded.startsWith("/") || decoded.contains('\\') || decoded.any { it == '\u0000' || it.code < 32 }) throw ImageProblem(ImageFailure.OutsideFolder)
    val result = mutableListOf<String>()
    decoded.split('/').forEach { segment -> when (segment) {
        "", "." -> Unit
        ".." -> if (result.isEmpty()) throw ImageProblem(ImageFailure.OutsideFolder) else result.removeAt(result.lastIndex)
        else -> result += segment
    } }
    if (result.isEmpty()) throw ImageProblem(ImageFailure.Missing)
    return result
}
internal fun checkedImageUrl(value: String): URI {
    val uri = try { URI(value) } catch (_: Exception) { throw ImageProblem(ImageFailure.Unsupported) }
    if (!uri.scheme.equals("https", true) || uri.host.isNullOrEmpty() || uri.rawUserInfo != null) throw ImageProblem(ImageFailure.Unsupported)
    return uri
}
internal fun readImageBytes(input: InputStream, declaredLength: Long = -1, checkDeadline: () -> Unit = {}): ByteArray {
    if (declaredLength > IMAGE_BYTE_LIMIT) throw ImageProblem(ImageFailure.TooLarge)
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        checkDeadline()
        val count = input.read(buffer)
        if (count < 0) break
        if (output.size().toLong() + count > IMAGE_BYTE_LIMIT) throw ImageProblem(ImageFailure.TooLarge)
        output.write(buffer, 0, count)
    }
    checkDeadline()
    return output.toByteArray()
}
internal fun checkImageDimensions(width: Int, height: Int) {
    if (width <= 0 || height <= 0) throw ImageProblem(ImageFailure.Decode)
    if (width.toLong() * height > IMAGE_PIXEL_LIMIT) throw ImageProblem(ImageFailure.TooLarge)
}
