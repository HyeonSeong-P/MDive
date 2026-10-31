package com.parka.mdive.data.document

import com.parka.mdive.domain.document.LinkFailure
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal class LinkProblem(val reason: LinkFailure) : Exception()

/** Decode percent octets once with strict UTF-8; '+' is a literal file-name character. */
internal fun linkSegments(destination: String, directory: List<String> = emptyList()): List<String> {
    if (destination.contains('#')) throw LinkProblem(LinkFailure.Fragment)
    if (destination.contains('?') || destination.startsWith('/') || destination.startsWith('\\') || Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(destination)) throw LinkProblem(LinkFailure.OutsideFolder)
    val decoded = buildString {
        var i = 0
        while (i < destination.length) {
            if (destination[i] != '%') { append(destination[i++]); continue }
            val bytes = ByteArrayOutputStream()
            while (i < destination.length && destination[i] == '%') {
                if (i + 2 >= destination.length) throw LinkProblem(LinkFailure.OutsideFolder)
                val value = destination.substring(i + 1, i + 3).toIntOrNull(16) ?: throw LinkProblem(LinkFailure.OutsideFolder)
                bytes.write(value); i += 3
            }
            try { append(Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray()))) }
            catch (_: Exception) { throw LinkProblem(LinkFailure.OutsideFolder) }
        }
    }
    if (decoded.startsWith('/') || decoded.contains('\\') || decoded.any { it.code < 32 || it.code == 127 }) throw LinkProblem(LinkFailure.OutsideFolder)
    val result = directory.toMutableList()
    decoded.split('/').forEach { segment -> when (segment) {
        "", "." -> Unit
        ".." -> if (result.isEmpty()) throw LinkProblem(LinkFailure.OutsideFolder) else result.removeAt(result.lastIndex)
        else -> result += segment
    } }
    if (result.isEmpty() || result.last().substringAfterLast('.', "").lowercase(java.util.Locale.ROOT) !in setOf("md", "markdown")) throw LinkProblem(LinkFailure.Unsupported)
    return result
}

internal fun checkedWebLink(destination: String): Boolean = try {
    val uri = java.net.URI(destination)
    uri.scheme?.lowercase(java.util.Locale.ROOT) in setOf("http", "https") && !uri.host.isNullOrBlank() && uri.rawUserInfo == null
} catch (_: Exception) { false }
