package com.parka.mdive.ui

import com.parka.mdive.ui.reader.*
import org.junit.Assert.*
import org.junit.Test

class MermaidCacheTest {
    @Test fun cacheSeparatesSourceThemeProfileAndRenderWidth() {
        val cache = MermaidSessionCache()
        val key = MermaidCacheKey("graph TD; A-->B", false)
        val ready = MermaidPrepared.Ready("<svg viewBox='0 0 1 1'/>")
        cache.put(key, ready)
        assertSame(ready, cache[key])
        assertNull(cache[key.copy(source = "graph TD; A-->C")])
        assertNull(cache[key.copy(dark = true)])
        assertNull(cache[key.copy(profile = "next")])
        assertNull(cache[key.copy(renderWidth = 2048)])
        assertNull(MermaidSessionCache()[key])
    }

    @Test fun boundedLruCountsKeyBytesAndDoesNotRetainOversizedEntry() {
        val cache = MermaidSessionCache(maxBytes = 512, maxEntries = 2)
        val a = MermaidCacheKey("a", false)
        val b = a.copy(source = "b")
        val c = a.copy(source = "c")
        val value = MermaidPrepared.Ready("<svg/>")
        cache.put(a, value); cache.put(b, value)
        assertSame(value, cache[a])
        cache.put(c, value)
        assertNull(cache[b]); assertSame(value, cache[a]); assertSame(value, cache[c])
        cache.put(a.copy(source = "x".repeat(300)), MermaidPrepared.Failure)
        assertTrue(cache.retainedBytes <= 512)
        assertSame(value, cache[a])
    }

    @Test fun failureIsRetainedAndSessionDoesNotRetainInFlightMarkers() {
        val cache = MermaidSessionCache()
        val key = MermaidCacheKey("flowchart TD\nA[unfinished", false)
        assertNull(cache[key])
        cache.put(key, MermaidPrepared.Failure)
        assertSame(MermaidPrepared.Failure, cache[key])
        assertNull(cache[key.copy(dark = true)])
    }
}
