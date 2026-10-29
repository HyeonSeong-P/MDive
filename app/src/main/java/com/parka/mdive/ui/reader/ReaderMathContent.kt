package com.parka.mdive.ui.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.compose.material3.MaterialTheme
import com.parka.mdive.designsystem.*
import com.parka.mdive.domain.document.*

@Composable
internal fun ReaderMathBlock(math: MarkdownMath, onViewSource: (String) -> Unit, modifier: Modifier = Modifier, style: TextStyle = mdiveTextStyle("body")) {
    if (math.raster == null) MDiveContentFallback("수식을 표시하지 못했어요", "지원 범위나 처리 한계를 벗어난 수식이에요.", modifier.testTag("math-failure"), onViewSource = { onViewSource(math.source) })
    else MDiveRenderedBlock(MDiveResourceState.Ready, "수식: ${math.tex}", modifier.testTag("math-block")) {
        MathImage(math.raster, style, Modifier)
    }
}

/** Large/failed math leaves the inline flow; surrounding text stays in reading order. */
@Composable
internal fun ReaderMathText(runs: List<MarkdownRun>, width: Dp, onViewSource: (String) -> Unit, modifier: Modifier = Modifier,
    style: TextStyle = mdiveTextStyle("body"), textAlign: TextAlign = TextAlign.Start) {
    val density = LocalDensity.current
    val size = style.fontSize
    val widthSp = with(density) { width.toSp().value }
    val groups = mutableListOf<List<MarkdownRun>>()
    var pending = mutableListOf<MarkdownRun>()
    runs.forEach { run ->
        val math = run.math
        if (math != null && (math.raster == null || math.raster.width.toFloat() / math.raster.emPixels * size.value > widthSp)) {
            if (pending.isNotEmpty()) groups += pending.toList()
            groups += listOf(run)
            pending = mutableListOf()
        } else pending += run
    }
    if (pending.isNotEmpty()) groups += pending
    Column(modifier.fillMaxWidth()) {
        groups.forEach { group ->
            val only = group.singleOrNull()?.math
            if (only != null && (only.raster == null || only.raster.width.toFloat() / only.raster.emPixels * size.value > widthSp)) {
                ReaderMathBlock(only, onViewSource, style = style)
            } else InlineMathText(group, style, textAlign)
        }
    }
}

@Composable
private fun InlineMathText(runs: List<MarkdownRun>, style: TextStyle, textAlign: TextAlign) {
    val inlines = mutableMapOf<String, InlineTextContent>()
    var largestHeight = style.lineHeight.value
    val text = buildAnnotatedString {
        runs.forEachIndexed { index, run ->
            val math = run.math
            val raster = math?.raster
            if (raster != null) {
                val scale = style.fontSize.value / raster.emPixels
                val ascent = (raster.height - raster.depth) * scale
                largestHeight = maxOf(largestHeight, (raster.height + raster.depth) * scale)
                val id = "math-$index"
                inlines[id] = InlineTextContent(Placeholder((raster.width * scale).sp, ascent.coerceAtLeast(1f).sp, PlaceholderVerticalAlign.AboveBaseline)) {
                    // Placeholder bottom is the text baseline; draw depth below it without clipping.
                    MathImage(raster, style, Modifier, includeDepthInLayout = false)
                }
                appendInlineContent(id, "수식: ${math.tex}")
            } else withStyle(SpanStyle(
                fontWeight = if (run.strong) FontWeight.SemiBold else null,
                fontStyle = if (run.emphasis) FontStyle.Italic else null,
                fontFamily = if (run.code) mdiveTextStyle("code").fontFamily else null,
                background = if (run.code) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Unspecified,
                textDecoration = if (run.strike) TextDecoration.LineThrough else null,
            )) { append(run.text) }
        }
    }
    MDiveMathInline(text, inlines, Modifier.fillMaxWidth().testTag("math-inline"), style.copy(lineHeight = largestHeight.sp), textAlign)
}

@Composable
private fun MathImage(raster: MathRaster, style: TextStyle, modifier: Modifier, includeDepthInLayout: Boolean = true) {
    val bitmap = remember(raster) { Bitmap.createBitmap(raster.copyPixels(), raster.width, raster.height, Bitmap.Config.ARGB_8888).asImageBitmap() }
    val color = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val width = with(density) { (raster.width.toFloat() / raster.emPixels * style.fontSize.value).sp.toDp() }
    val height = with(density) { (raster.height.toFloat() / raster.emPixels * style.fontSize.value).sp.toDp() }
    val layoutHeight = if (includeDepthInLayout) height else height * (raster.height - raster.depth).toFloat() / raster.height
    Canvas(modifier.requiredSize(width, layoutHeight)) {
        drawImage(bitmap, dstSize = IntSize(width.roundToPx(), height.roundToPx()), colorFilter = ColorFilter.tint(color))
    }
}
