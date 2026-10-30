package com.parka.mdive.data.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil3.toBitmap
import coil3.asImage
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.request.maxBitmapSize
import android.util.Xml
import com.caverock.androidsvg.SVG
import com.parka.mdive.domain.document.ImageFailure
import com.parka.mdive.domain.document.ImageRaster
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import kotlin.math.ceil
import okio.Path.Companion.toOkioPath
import okio.buffer
import kotlinx.coroutines.ensureActive

/** The fetch model is data-internal; Coil never leaks into domain or screen state. */
private data class PreparedImage(val vector: Boolean, val width: Int, val height: Int)
private class ImageInput(val key: String, val web: Boolean, val read: () -> ByteArray) { var prepared: PreparedImage? = null }
internal class ImageDecoder(
    private val context: android.content.Context,
    memoryBytes: Long = 64L * 1024 * 1024,
    diskDirectory: java.io.File = java.io.File(context.cacheDir, "coil-web-images"),
    diskBytes: Long = 100L * 1024 * 1024,
) {
    internal val fetchStarts = java.util.concurrent.atomic.AtomicInteger()
    internal val decodeStarts = java.util.concurrent.atomic.AtomicInteger()
    private val loader = coil3.ImageLoader.Builder(context.applicationContext)
        .memoryCache { coil3.memory.MemoryCache.Builder().maxSizeBytes(memoryBytes).weakReferencesEnabled(false).build() }
        .diskCache { coil3.disk.DiskCache.Builder().directory(diskDirectory.toOkioPath()).maxSizeBytes(diskBytes).build() }
        .components { add(object : coil3.fetch.Fetcher.Factory<ImageInput> {
            override fun create(data: ImageInput, options: coil3.request.Options, imageLoader: coil3.ImageLoader) = coil3.fetch.Fetcher {
                val disk = imageLoader.diskCache.takeIf { data.web }
                var bytes: ByteArray? = null
                var source = coil3.decode.DataSource.NETWORK
                if (disk != null && options.diskCachePolicy.readEnabled) {
                    disk.openSnapshot(data.key)?.use { snapshot ->
                        bytes = disk.fileSystem.source(snapshot.data).buffer().inputStream().use { readImageBytes(it) }
                    }
                    if (bytes != null) source = coil3.decode.DataSource.DISK
                }
                if (bytes == null) { fetchStarts.incrementAndGet(); bytes = data.read() }
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val prepared = requireNotNull(bytes)
                val checked = preflight(prepared)
                data.prepared = checked
                if (disk != null && source != coil3.decode.DataSource.DISK && options.diskCachePolicy.writeEnabled) {
                    disk.openEditor(data.key)?.let { editor ->
                        var committed = false
                        try {
                            disk.fileSystem.sink(editor.metadata).buffer().use { }
                            disk.fileSystem.sink(editor.data).buffer().use { it.write(prepared) }
                            kotlinx.coroutines.currentCoroutineContext().ensureActive()
                            editor.commit()
                            committed = true
                        } finally { if (!committed) editor.abort() }
                    }
                }
                coil3.fetch.SourceFetchResult(coil3.decode.ImageSource(okio.Buffer().write(prepared), options.fileSystem),
                    if (checked.vector) "image/svg+xml" else "image/raster", source)
            }
        }) }
        .build()

    /** Test/fixture entry also exercises the actual Coil fetch/decode pipeline. */
    suspend fun decode(bytes: ByteArray): ImageRaster = load(sha256(bytes), false, false) { bytes }

    suspend fun load(key: String, web: Boolean, bypass: Boolean, read: () -> ByteArray): ImageRaster {
        val policy = if (bypass) coil3.request.CachePolicy.WRITE_ONLY else coil3.request.CachePolicy.ENABLED
        val input = ImageInput(key, web, read)
        val request = coil3.request.ImageRequest.Builder(context)
            .data(input).size(coil3.size.Size.ORIGINAL)
            .allowHardware(false).bitmapConfig(Bitmap.Config.ARGB_8888)
            .maxBitmapSize(coil3.size.Size(16_000_000, 16_000_000))
            .memoryCacheKey(key).diskCacheKey(key)
            .memoryCachePolicy(policy)
            .diskCachePolicy(if (web) policy else coil3.request.CachePolicy.DISABLED)
            .decoderFactory { result, options, imageLoader ->
                decodeStarts.incrementAndGet()
                if (result.mimeType == "image/svg+xml") object : coil3.decode.Decoder {
                    override suspend fun decode(): coil3.decode.DecodeResult? {
                        val dimensions = requireNotNull(input.prepared)
                        val decoded = coil3.svg.SvgDecoder(result.source, options, useViewBoundsAsIntrinsicSize = false, renderToBitmap = false).decode() ?: return null
                        val bitmap = decoded.image.toBitmap(dimensions.width, dimensions.height, Bitmap.Config.ARGB_8888)
                        checkImageDimensions(bitmap.width, bitmap.height)
                        // This is the full validated original raster, not a sampled thumbnail.
                        // Coil's native cache can therefore satisfy an ORIGINAL request on return.
                        return coil3.decode.DecodeResult(bitmap.asImage(), isSampled = false)
                    }
                } else coil3.decode.BitmapFactoryDecoder.Factory().create(result, options, imageLoader)
            }.build()
        val result = loader.execute(request)
        if (result !is coil3.request.SuccessResult) {
            val error = (result as? coil3.request.ErrorResult)?.throwable
            when (error) {
                is OutOfMemoryError -> throw ImageProblem(ImageFailure.TooLarge)
                is StackOverflowError -> throw ImageProblem(ImageFailure.Unsupported)
                null -> throw ImageProblem(ImageFailure.Decode)
                else -> throw error // Preserve SAF access and network/timeout categories.
            }
        }
        val bitmap = result.image.toBitmap()
        checkImageDimensions(bitmap.width, bitmap.height)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // Coil owns cached bitmaps: never recycle a memory-cache entry.
        return ImageRaster(bitmap.width, bitmap.height, pixels)
    }
    internal fun clearMemory() { loader.memoryCache?.clear() }
    internal fun close() { loader.shutdown() }

    private fun preflight(bytes: ByteArray): PreparedImage {
        if (bytes.size > IMAGE_BYTE_LIMIT) throw ImageProblem(ImageFailure.TooLarge)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth > 0) {
            if (bounds.outMimeType !in setOf("image/png", "image/jpeg", "image/webp", "image/gif")) throw ImageProblem(ImageFailure.Unsupported)
            checkImageDimensions(bounds.outWidth, bounds.outHeight)
            return PreparedImage(false, bounds.outWidth, bounds.outHeight)
        }
        if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) throw ImageProblem(ImageFailure.Unsupported)
        validateSvg(bytes)
        SVG.setInternalEntitiesEnabled(false)
        val svg = SVG.getFromInputStream(ByteArrayInputStream(bytes))
        val viewBox = svg.documentViewBox
        val width = svg.documentWidth.takeIf { it.isFinite() && it > 0 } ?: viewBox?.width() ?: throw ImageProblem(ImageFailure.Decode)
        val height = svg.documentHeight.takeIf { it.isFinite() && it > 0 } ?: viewBox?.height() ?: throw ImageProblem(ImageFailure.Decode)
        if (!width.isFinite() || !height.isFinite() || width > IMAGE_PIXEL_LIMIT || height > IMAGE_PIXEL_LIMIT) throw ImageProblem(ImageFailure.TooLarge)
        val w = ceil(width.toDouble()).toInt(); val h = ceil(height.toDouble()).toInt()
        checkImageDimensions(w, h)
        return PreparedImage(true, w, h)
    }
}

/** Token validation is encoding-aware, including UTF-16 XML, before the SVG engine sees bytes. */
internal fun validateSvg(bytes: ByteArray) {
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
    parser.setInput(ByteArrayInputStream(bytes), null)
    var depth = 0
    var root = false
    var styleDepth = -1
    val styleText = StringBuilder()
    val edges = mutableListOf<MutableList<Int>>()
    val stack = mutableListOf<Int>()
    val ids = mutableMapOf<String, Int>()
    val references = mutableListOf<Pair<Int, String>>()
    val allowed = setOf("svg", "g", "defs", "symbol", "use", "path", "rect", "circle", "ellipse", "line", "polyline", "polygon", "text", "tspan", "textPath", "linearGradient", "radialGradient", "stop", "clipPath", "mask", "pattern", "marker", "title", "desc", "switch", "style")
    fun checkCss(value: String, owner: Int) {
        if (value.contains('@') || value.contains('\\')) throw ImageProblem(ImageFailure.Unsupported)
        Regex("url\\s*\\((.*?)\\)", RegexOption.IGNORE_CASE).findAll(value).forEach {
            val target = it.groupValues[1].trim().trim('\'', '"')
            if (!target.startsWith('#') || target.length < 2) throw ImageProblem(ImageFailure.Unsupported)
            references += owner to target.drop(1)
        }
    }
    try {
        while (true) {
            when (parser.nextToken()) {
                XmlPullParser.END_DOCUMENT -> break
                XmlPullParser.DOCDECL, XmlPullParser.PROCESSING_INSTRUCTION -> throw ImageProblem(ImageFailure.Unsupported)
                XmlPullParser.ENTITY_REF -> {
                    if (parser.name !in setOf("amp", "lt", "gt", "apos", "quot")) throw ImageProblem(ImageFailure.Unsupported)
                    if (styleDepth > 0) styleText.append(parser.text ?: "")
                }
                XmlPullParser.START_TAG -> {
                    if (styleDepth > 0) throw ImageProblem(ImageFailure.Unsupported)
                    if (++depth > 128 || edges.size >= 10_000 || parser.name !in allowed) throw ImageProblem(ImageFailure.Unsupported)
                    val node = edges.size
                    edges += mutableListOf<Int>()
                    stack.lastOrNull()?.let { edges[it] += node }
                    stack += node
                    if (parser.name == "style") { styleDepth = depth; styleText.setLength(0) }
                    if (!root) { if (parser.name != "svg") throw ImageProblem(ImageFailure.Unsupported); root = true }
                    if (parser.namespace !in listOf("", "http://www.w3.org/2000/svg")) throw ImageProblem(ImageFailure.Unsupported)
                    for (index in 0 until parser.attributeCount) {
                        val name = parser.getAttributeName(index)
                        val value = parser.getAttributeValue(index)
                        if (name.startsWith("on", true) || name.equals("base", true)) throw ImageProblem(ImageFailure.Unsupported)
                        if (name == "id") {
                            if (ids.put(value, node) != null) throw ImageProblem(ImageFailure.Unsupported)
                        }
                        if (name.equals("href", true)) {
                            if (!value.startsWith('#') || value.length < 2) throw ImageProblem(ImageFailure.Unsupported)
                            references += node to value.drop(1)
                        }
                        checkCss(value, node)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (depth == styleDepth) {
                        val css = styleText.toString().replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
                        // Selector matching is engine-owned; do not accept hidden URL dependencies.
                        if (Regex("url\\s*\\(", RegexOption.IGNORE_CASE).containsMatchIn(css)) throw ImageProblem(ImageFailure.Unsupported)
                        checkCss(css, stack.last())
                        styleDepth = -1
                    }
                    depth--; stack.removeAt(stack.lastIndex)
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (styleDepth > 0) styleText.append(parser.text ?: "")
            }
        }
        if (!root || references.size > 10_000) throw ImageProblem(ImageFailure.Unsupported)
        references.forEach { (from, target) -> ids[target]?.let { edges[from] += it } }
        val visiting = IntArray(edges.size)
        val costs = LongArray(edges.size)
        fun visit(node: Int, level: Int): Long {
            if (level > 128 || visiting[node] == 1) throw ImageProblem(ImageFailure.Unsupported)
            if (visiting[node] == 2) return costs[node]
            visiting[node] = 1
            var cost = 1L
            edges[node].forEach { child ->
                cost += visit(child, level + 1)
                if (cost > 50_000) throw ImageProblem(ImageFailure.Unsupported)
            }
            visiting[node] = 2
            costs[node] = cost
            return cost
        }
        edges.indices.forEach { visit(it, 0) }
    } catch (e: ImageProblem) { throw e } catch (_: Exception) { throw ImageProblem(ImageFailure.Unsupported) }
}
