package com.parka.mdive.ui.reader

/** Main-thread, Reader-owned cache. View instances and in-flight work are never retained. */
internal class MermaidSessionCache(private val maxBytes: Int = 4 * 1024 * 1024, private val maxEntries: Int = 64) {
    private val entries = LinkedHashMap<MermaidCacheKey, MermaidPrepared>(16, .75f, true)
    internal var renderStarts: Int = 0
        private set
    fun recordRenderStart() { renderStarts++ }
    internal var retainedBytes: Int = 0
        private set

    operator fun get(key: MermaidCacheKey): MermaidPrepared? = entries[key]
    fun put(key: MermaidCacheKey, value: MermaidPrepared) {
        entries.remove(key)?.let { retainedBytes -= weight(key, it) }
        val bytes = weight(key, value)
        if (bytes > maxBytes) return
        entries[key] = value
        retainedBytes += bytes
        while (retainedBytes > maxBytes || entries.size > maxEntries) {
            val oldest = entries.entries.iterator().next()
            retainedBytes -= weight(oldest.key, oldest.value)
            entries.remove(oldest.key)
        }
    }
    private fun weight(key: MermaidCacheKey, value: MermaidPrepared): Int =
        64 + key.source.length * 2 + (if (value is MermaidPrepared.Ready) value.svg.length * 2 else 0)
}

/** Layout uses a fixed off-screen width; output viewBox scales independently of display width. */
internal data class MermaidCacheKey(
    val source: String,
    val dark: Boolean,
    val profile: String = "mermaid-11.15.0-s07-v1",
    val renderWidth: Int = MERMAID_RENDER_WIDTH,
)
internal const val MERMAID_RENDER_WIDTH = 1024
internal sealed interface MermaidPrepared {
    data class Ready(val svg: String) : MermaidPrepared
    data object Failure : MermaidPrepared
}
