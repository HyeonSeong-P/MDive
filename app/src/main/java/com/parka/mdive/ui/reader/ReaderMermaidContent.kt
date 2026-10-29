package com.parka.mdive.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.parka.mdive.designsystem.*
import com.parka.mdive.domain.document.MarkdownMermaid

@Composable
private fun rememberEngine(diagram: MarkdownMermaid, cache: MermaidSessionCache): Pair<MermaidWebView?, MermaidRenderState> {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.red < 0.5f
    val key = remember(diagram.code, dark) { MermaidCacheKey(diagram.code, dark) }
    val prepared = cache[key]
    var state by remember(key, cache) { mutableStateOf(when {
        !diagram.supported || prepared == MermaidPrepared.Failure -> MermaidRenderState.Failure
        prepared is MermaidPrepared.Ready -> MermaidRenderState.Ready
        else -> MermaidRenderState.Loading
    }) }
    val engine = remember(key, cache, context) {
        if (!diagram.supported || prepared == MermaidPrepared.Failure) null
        else runCatching {
            MermaidWebView(context, diagram.code, dark,
                cachedSvg = (prepared as? MermaidPrepared.Ready)?.svg,
                onPreparedSvg = { cache.put(key, MermaidPrepared.Ready(it)) },
                onRenderStart = cache::recordRenderStart,
                onResult = { result ->
                    if (result == MermaidRenderState.Failure) cache.put(key, MermaidPrepared.Failure)
                    state = result
                })
        }.getOrElse { cache.put(key, MermaidPrepared.Failure); state = MermaidRenderState.Failure; null }
    }
    DisposableEffect(engine) { onDispose { engine?.close() } }
    return engine to state
}

@Composable
internal fun ReaderMermaidBlock(diagram: MarkdownMermaid, availableWidth: Dp, onSource: (String) -> Unit, onExpand: (MarkdownMermaid) -> Unit, cache: MermaidSessionCache) {
    val (engine, state) = rememberEngine(diagram, cache)
    when (state) {
        MermaidRenderState.Failure -> MDiveContentFallback("다이어그램을 표시하지 못했어요", "지원 범위나 처리 한계를 벗어났거나 렌더링에 실패했어요.", Modifier.testTag("mermaid-failure")) { onSource(diagram.source) }
        MermaidRenderState.Loading -> MermaidSkeleton(Modifier.fillMaxWidth().height(240.dp).testTag("mermaid-loading"))
        MermaidRenderState.Ready -> MDiveRenderedBlock(MDiveResourceState.Ready, "Mermaid 다이어그램", Modifier.testTag("mermaid-ready"), onViewSource = { onSource(diagram.source) }) {
            Box(Modifier.width(availableWidth.coerceAtLeast(1.dp)).height(240.dp).testTag("mermaid-picture-container")) {
                key(engine) { AndroidView(factory = { requireNotNull(engine).view }, modifier = Modifier.fillMaxSize()) }
                Box(Modifier.fillMaxSize().testTag("mermaid-picture").clickable(role = Role.Button, onClickLabel = "다이어그램 확대") { onExpand(diagram) }
                    .semantics { contentDescription = "Mermaid 다이어그램: ${diagram.code}" })
            }
        }
    }
}

@Composable
internal fun MermaidSkeleton(modifier: Modifier = Modifier) {
    Box(modifier.semantics {
        contentDescription = "Mermaid 다이어그램을 준비하고 있어요"
        stateDescription = "불러오는 중"
        liveRegion = LiveRegionMode.Polite
    }) { MDiveSkeleton(Modifier.fillMaxSize()) }
}

@Composable
internal fun MermaidExpanded(diagram: MarkdownMermaid, onClose: () -> Unit, onViewSource: (String) -> Unit, cache: MermaidSessionCache) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().testTag("mermaid-expanded")) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                MDiveTopBar("Mermaid 확대", navigationIcon = "close", navigationDescription = "확대 닫기", onNavigationClick = onClose)
                val (engine, state) = rememberEngine(diagram, cache)
                var transform by remember { mutableStateOf(DiagramTransform()) }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (state) {
                        MermaidRenderState.Failure -> MDiveContentFallback("다이어그램을 표시하지 못했어요", "지원 범위나 처리 한계를 벗어났거나 렌더링에 실패했어요.", Modifier.padding(16.dp), onViewSource = { onViewSource(diagram.source) })
                        MermaidRenderState.Loading -> MermaidSkeleton(Modifier.fillMaxSize().padding(16.dp).testTag("mermaid-expanded-loading"))
                        MermaidRenderState.Ready -> BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().testTag("mermaid-viewport")
                            .semantics { contentDescription = "확대된 Mermaid 다이어그램: ${diagram.code}"; stateDescription = "${transform.scale}배" }) {
                            val width = constraints.maxWidth.toFloat()
                            val height = constraints.maxHeight.toFloat()
                            key(engine) { AndroidView(factory = { requireNotNull(engine).view }, modifier = Modifier.fillMaxSize().graphicsLayer {
                                scaleX = transform.scale; scaleY = transform.scale
                                translationX = transform.x.coerceIn(-width * (transform.scale-1)/2, width * (transform.scale-1)/2)
                                translationY = transform.y.coerceIn(-height * (transform.scale-1)/2, height * (transform.scale-1)/2)
                            }) }
                            Box(Modifier.fillMaxSize().pointerInput(width, height) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    transform = transform.change(zoom, pan, centroid - Offset(width/2, height/2), width, height)
                                }
                            })
                        }
                    }
                }
                if (state == MermaidRenderState.Ready) MermaidZoomControls(transform, { transform = it })
            }
        }
    }
}

@Composable
internal fun MermaidZoomControls(transform: DiagramTransform, onChange: (DiagramTransform) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Surface(shape = androidx.compose.foundation.shape.CircleShape,
            color = colors.surfaceVariant, contentColor = colors.onSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)) {
            Row(Modifier.padding(4.dp).testTag("mermaid-zoom-controls"), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                MDiveIconButton("minus", "축소", { onChange(transform.zoomBy(0.5f)) }, enabled = transform.scale > 1f, tint = colors.onSurfaceVariant)
                androidx.compose.material3.Text("${kotlin.math.round(transform.scale * 100).toInt()}%",
                    Modifier.widthIn(min = 64.dp).semantics { contentDescription = "맞춤 대비 ${transform.scale}배" },
                    style = mdiveTextStyle("label").copy(fontFeatureSettings = "tnum"), maxLines = 1, softWrap = false,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                MDiveIconButton("plus", "확대", { onChange(transform.zoomBy(2f)) }, enabled = transform.scale < 8f, tint = colors.onSurfaceVariant)
                Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(24.dp).background(colors.outlineVariant))
                MDiveIconButton("fit", "전체 맞춤", { onChange(DiagramTransform()) }, tint = colors.onSurfaceVariant)
            }
        }
    }
}

/** Pure viewport transform, bounded to the agreed fit-relative scale and visible viewport. */
internal data class DiagramTransform(val scale: Float = 1f, val x: Float = 0f, val y: Float = 0f) {
    fun zoomBy(factor: Float): DiagramTransform = copy(scale = (scale * factor).coerceIn(1f, 8f), x = 0f, y = 0f)
    fun change(factor: Float, pan: Offset, focus: Offset, width: Float, height: Float): DiagramTransform {
        val next = (scale * factor).coerceIn(1f, 8f)
        if (next == 1f) return DiagramTransform()
        val ratio = next / scale
        return DiagramTransform(next,
            (x * ratio + focus.x * (1-ratio) + pan.x).coerceIn(-width*(next-1)/2, width*(next-1)/2),
            (y * ratio + focus.y * (1-ratio) + pan.y).coerceIn(-height*(next-1)/2, height*(next-1)/2))
    }
}
