package com.parka.mdive.data.document

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

internal const val MAX_DOCUMENT_BYTES = 5 * 1024 * 1024
internal class DocumentTooLargeException : Exception()

/** Checks the actual stream, including providers that omit or misreport SIZE. */
internal suspend fun readBoundedSource(stream: InputStream): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        coroutineContext.ensureActive()
        val count = stream.read(buffer, 0, minOf(buffer.size, MAX_DOCUMENT_BYTES + 1 - output.size()))
        coroutineContext.ensureActive()
        if (count < 0) return output.toByteArray()
        if (count == 0) continue
        output.write(buffer, 0, count)
        if (output.size() > MAX_DOCUMENT_BYTES) throw DocumentTooLargeException()
    }
}

/** BOM is consumed once; malformed input never becomes silent replacement characters. */
internal fun decodeSource(bytes: ByteArray): String {
    val (charset, offset) = when {
        bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> StandardCharsets.UTF_8 to 3
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> StandardCharsets.UTF_16LE to 2
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> StandardCharsets.UTF_16BE to 2
        else -> StandardCharsets.UTF_8 to 0
    }
    return charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes, offset, bytes.size - offset)).toString()
}
