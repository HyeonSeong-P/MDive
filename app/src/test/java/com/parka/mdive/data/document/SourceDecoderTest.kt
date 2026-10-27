package com.parka.mdive.data.document

import java.io.ByteArrayInputStream
import java.nio.charset.CharacterCodingException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SourceDecoderTest {
    @Test fun utf8AndUtf16PreserveWhitespaceAndConsumeOnlyLeadingBom() {
        val source = "# 한글\r\n  공백\t\n\uFEFF끝\n"
        assertEquals(source, decodeSource(source.toByteArray()))
        assertEquals(source, decodeSource(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + source.toByteArray()))
        assertEquals(source, decodeSource(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + source.toByteArray(Charsets.UTF_16LE)))
        assertEquals(source, decodeSource(byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + source.toByteArray(Charsets.UTF_16BE)))
        assertEquals("\u0000A", decodeSource(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0, 0, 65, 0)))
        assertEquals("", decodeSource(byteArrayOf(0xFF.toByte(), 0xFE.toByte())))
    }

    @Test fun malformedUtf8AndUtf16FailWithoutReplacingCharacters() {
        assertThrows(CharacterCodingException::class.java) { decodeSource(byteArrayOf(0xC3.toByte(), 0x28)) }
        assertThrows(CharacterCodingException::class.java) { decodeSource(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00)) }
        assertThrows(CharacterCodingException::class.java) { decodeSource(byteArrayOf(0xFE.toByte(), 0xFF.toByte(), 0xD8.toByte(), 0x00)) }
    }

    @Test fun streamBoundaryIsInclusiveAndDoesNotDependOnMetadata() = runTest {
        val maximum = ByteArray(MAX_DOCUMENT_BYTES) { 65 }
        assertArrayEquals(maximum, readBoundedSource(ByteArrayInputStream(maximum)))
        try {
            readBoundedSource(ByteArrayInputStream(maximum + byteArrayOf(65)))
            throw AssertionError("Expected size failure")
        } catch (_: DocumentTooLargeException) { }
    }
}
