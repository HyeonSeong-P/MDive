package com.parka.mdive.ui.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.parka.mdive.designsystem.*
import com.parka.mdive.domain.document.*

internal class ReaderImages(
    val results: Map<String, ImageReadResult> = emptyMap(),
    val generation: Long = 0,
    val onLoad: (MarkdownImage, Boolean) -> Unit = { _, _ -> },
    val onVisible: (MarkdownImage, Boolean) -> Unit = { _, _ -> },
    val onConnect: () -> Unit = {},
    val onExpand: (MarkdownImage, ImageRaster) -> Unit = { _, _ -> },
)

@Composable
internal fun ReaderImageBlock(image: MarkdownImage, images: ReaderImages, modifier: Modifier = Modifier) {
    val result = images.results[image.destination]
    DisposableEffect(image.destination, images.generation) {
        images.onVisible(image, true)
        onDispose { images.onVisible(image, false) }
    }
    val raster = (result as? ImageReadResult.Ready)?.raster
    val bitmap = raster?.let { rememberImageBitmap(it) }
    val ready = raster != null && bitmap != null
    val failure = if (raster != null && bitmap == null) ImageFailure.TooLarge else (result as? ImageReadResult.Failure)?.reason
    val localRecovery = !image.destination.startsWith("https:", true) && failure in setOf(ImageFailure.FolderRequired, ImageFailure.Access, ImageFailure.Missing)
    MDiveMarkdownImage(
        state = when { ready -> MDiveResourceState.Ready; result == null -> MDiveResourceState.Loading; localRecovery -> MDiveResourceState.AccessRequired; else -> MDiveResourceState.Failure },
        alt = image.alt.ifBlank { "이미지" }, title = image.title,
        modifier = modifier.testTag(when { ready -> "image-ready"; result == null -> "image-loading"; else -> "image-failure" }),
        onExpand = if (ready) { { images.onExpand(image, requireNotNull(raster)) } } else null,
        onConnect = if (localRecovery) images.onConnect else null,
        onRetry = if (image.destination.startsWith("https:", true) && failure != null) { { images.onLoad(image, true) } } else null,
        failureReason = failure?.description(),
    ) {
        if (ready) RasterImage(requireNotNull(raster), requireNotNull(bitmap), Modifier.fillMaxWidth().heightIn(max = 360.dp).testTag("image-picture"))
    }
}

/** Weak presentation sharing adds no second retention cache; viewport and overlay use one bitmap. */
private val presentationBitmaps = java.util.WeakHashMap<ImageRaster, java.lang.ref.WeakReference<androidx.compose.ui.graphics.ImageBitmap>>()
@Composable
private fun rememberImageBitmap(raster: ImageRaster): androidx.compose.ui.graphics.ImageBitmap? = remember(raster) {
    presentationBitmaps[raster]?.get() ?: try {
        Bitmap.createBitmap(raster.pixels, raster.width, raster.height, Bitmap.Config.ARGB_8888).asImageBitmap().also {
            presentationBitmaps[raster] = java.lang.ref.WeakReference(it)
        }
    } catch (_: OutOfMemoryError) { null }
}
@Composable
private fun RasterImage(raster: ImageRaster, bitmap: androidx.compose.ui.graphics.ImageBitmap, modifier: Modifier = Modifier) {
    Image(bitmap, null, modifier.aspectRatio(raster.width.toFloat() / raster.height), contentScale = ContentScale.Fit)
}

/** Images occupy their own row while text before and after retains its original formatting. */
@Composable
internal fun ReaderResourceText(runs: List<MarkdownRun>, width: Dp, onMath: (String) -> Unit, images: ReaderImages, modifier: Modifier = Modifier, style: TextStyle = mdiveTextStyle("body"), align: TextAlign = TextAlign.Start) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val groups = mutableListOf<List<MarkdownRun>>()
        var pending = mutableListOf<MarkdownRun>()
        runs.forEach { run ->
            if (run.image != null) {
                if (pending.isNotEmpty()) groups += pending.toList()
                pending = mutableListOf()
                groups += listOf(run)
            } else pending += run
        }
        if (pending.isNotEmpty()) groups += pending.toList()
        groups.forEach { group ->
            val image = group.singleOrNull()?.image
            if (image != null) ReaderImageBlock(image, images)
            else ReaderMathText(group, width, onMath, style = style, textAlign = align)
        }
    }
}

@Composable
internal fun ImageExpanded(image: MarkdownImage, images: ReaderImages, expandedRaster: ImageRaster?, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().testTag("image-expanded")) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                MDiveTopBar("이미지 확대", navigationIcon = "close", navigationDescription = "확대 닫기", onNavigationClick = onClose)
                var transform by remember { mutableStateOf(DiagramTransform()) }
                val raster = expandedRaster ?: (images.results[image.destination] as? ImageReadResult.Ready)?.raster
                LaunchedEffect(image.destination) { if (expandedRaster == null) images.onLoad(image, false) }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds().testTag("image-viewport").semantics {
                    contentDescription = image.alt.ifBlank { "이미지" }; stateDescription = "맞춤 대비 ${transform.scale}배"
                }) {
                    val width = constraints.maxWidth.toFloat()
                    val height = constraints.maxHeight.toFloat()
                    val bitmap = raster?.let { rememberImageBitmap(it) }
                    if (raster != null && bitmap != null) {
                        val fittedWidth = minOf(width, height * raster.width / raster.height)
                        val fittedHeight = minOf(height, width * raster.height / raster.width)
                        Box(Modifier.fillMaxSize().graphicsLayer {
                            scaleX = transform.scale; scaleY = transform.scale
                            translationX = transform.x.coerceIn(-((fittedWidth * transform.scale - width) / 2).coerceAtLeast(0f), ((fittedWidth * transform.scale - width) / 2).coerceAtLeast(0f))
                            translationY = transform.y.coerceIn(-((fittedHeight * transform.scale - height) / 2).coerceAtLeast(0f), ((fittedHeight * transform.scale - height) / 2).coerceAtLeast(0f))
                        }, contentAlignment = androidx.compose.ui.Alignment.Center) { RasterImage(raster, bitmap, Modifier.fillMaxSize().testTag("image-expanded-picture")) }
                        Box(Modifier.fillMaxSize().pointerInput(width, height) {
                            detectTransformGestures { centroid, pan, zoom, _ -> transform = transform.change(zoom, pan, centroid - Offset(width/2, height/2), width, height) }
                        })
                    } else if (raster != null) MDiveRecoveryBanner("이미지를 표시하지 못했어요", ImageFailure.TooLarge.description(), error = true)
                    else MDiveSkeleton(Modifier.fillMaxSize())
                }
                if (raster != null) MermaidZoomControls(transform, { transform = it }, Modifier.testTag("image-zoom-controls"))
            }
        }
    }
}
internal fun ImageFailure.description(): String = when (this) {
    ImageFailure.FolderRequired -> "이 문서의 이미지 폴더를 연결해주세요."
    ImageFailure.Access -> "폴더 접근 권한을 확인하고 이미지 폴더를 다시 연결해주세요."
    ImageFailure.Missing -> "연결한 폴더에 파일이 없어요. 문서 기준 폴더와 경로를 확인해주세요."
    ImageFailure.OutsideFolder -> "연결한 폴더 밖의 경로나 절대 경로는 지원하지 않아요."
    ImageFailure.Unsupported -> "지원하지 않는 형식이나 주소예요. 정적 PNG·JPEG·WebP·GIF·SVG와 HTTPS를 지원해요."
    ImageFailure.TooLarge -> "이미지가 20 MiB 또는 1,600만 픽셀 한계를 넘었어요."
    ImageFailure.Timeout -> "이미지를 기다리는 시간이 20초를 넘었어요. 연결을 확인하고 다시 시도해주세요."
    ImageFailure.Network -> "오프라인이거나 웹 이미지를 가져오지 못했어요. 연결을 확인하고 다시 시도해주세요."
    ImageFailure.Decode -> "이미지 파일을 표시하지 못했어요. 파일 상태를 확인해주세요."
}
