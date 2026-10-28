package com.parka.mdive.ui

import com.parka.mdive.ui.reader.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CodeCopyTest {
    @Test fun exactBodyWriteAndVersionSpecificFeedback() = runBlocking {
        val body = " \tval x = 1  \r\n\r\n"
        var copied: String? = null
        assertEquals(COPY_SUCCEEDED, copyCodeWithFeedback(32, body) { copied = it })
        assertEquals(body, copied)
        assertNull(copyCodeWithFeedback(33, body) { copied = it })
        assertEquals(COPY_FAILED, copyCodeWithFeedback(37, body) { throw SecurityException() })
        try { copyCodeWithFeedback(37, body) { throw CancellationException() }; fail("Cancellation swallowed") }
        catch (_: CancellationException) { }
    }
}
