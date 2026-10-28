package com.parka.mdive.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parka.mdive.designsystem.generated.CatalogSamples

@Composable
internal fun MarkdownCatalog(id: String, state: String) {
    var event by remember(id, state) { mutableStateOf("이벤트 대기 · 호스트 처리 시연") }
    val body = listOf(MDiveTextRun(CatalogSamples.markdownBody))
    MDiveMarkdownFlow {
        when (id) {
            "markdown-text" -> when (state) {
                "headings" -> (1..6).forEach { level ->
                    MDiveMarkdownSection(
                        heading = { MDiveMarkdownHeading(level, listOf(MDiveTextRun("H$level · ${CatalogSamples.markdownTitle}"))) },
                        spaceBefore = if (level == 1) 0.dp else 16.dp,
                    ) { MDiveMarkdownText(body) }
                }
                "inline" -> {
                    MDiveMarkdownText(listOf(MDiveTextRun("일반 한글 English 🙂 "), MDiveTextRun("강조와 굵게", emphasis = true, strong = true), MDiveTextRun(" 취소선", strike = true), MDiveTextRun(" val x = 1 ", code = true), MDiveTextRun("링크", strong = true, link = CatalogSamples.markdownLink), MDiveTextRun("\n줄바꿈 뒤 본문", highlighted = true)), onLink = { event = "링크: $it" })
                    MDiveMarkdownText(listOf(MDiveTextRun(CatalogSamples.markdownLink, link = CatalogSamples.markdownLink)), onLink = { event = "링크: $it" })
                }
                "divider" -> { MDiveMarkdownText(body); MDiveMarkdownDivider(); MDiveMarkdownText(body) }
            }
            "markdown-containers" -> when (state) {
                "quote" -> MDiveMarkdownQuote {
                    MDiveMarkdownText(body, style = mdiveTextStyle("documentQuote"))
                    MDiveMarkdownQuote { MDiveMarkdownText(listOf(MDiveTextRun("중첩 인용 · 깊이는 조합으로 표현합니다."))) }
                    MDiveMarkdownListItem(MDiveListMarker.Bullet) { MDiveMarkdownText(body) }
                    MDiveCodeBlock(CatalogSamples.code)
                }
                "lists" -> {
                    MDiveMarkdownListItem(MDiveListMarker.Ordered(98)) {
                        MDiveMarkdownText(body)
                        MDiveMarkdownListItem(MDiveListMarker.Bullet) { MDiveMarkdownText(body) }
                    }
                    MDiveMarkdownListItem(MDiveListMarker.Ordered(1000)) { MDiveMarkdownText(body) }
                    MDiveMarkdownListItem(MDiveListMarker.Task(false)) {
                        MDiveMarkdownText(listOf(MDiveTextRun("읽기 전용 부모 작업 · 여러 줄로 이어져도 첫 본문 줄에 상태를 맞춥니다.")))
                        MDiveMarkdownListItem(MDiveListMarker.Task(true)) {
                            MDiveMarkdownText(listOf(MDiveTextRun("완료한 자식 작업")))
                        }
                        MDiveMarkdownText(listOf(MDiveTextRun("같은 부모 항목의 두 번째 문단")))
                    }
                }
            }
            "markdown-resources" -> when (state) {
                "ready", "loading", "accessRequired", "failure" -> {
                    MDiveMarkdownText(body)
                    MDiveMarkdownImage(when (state) { "ready" -> MDiveResourceState.Ready; "loading" -> MDiveResourceState.Loading; "accessRequired" -> MDiveResourceState.AccessRequired; else -> MDiveResourceState.Failure }, CatalogSamples.markdownAlt,
                        title = "이미지 content slot · 로더 없음", onExpand = { event = "이미지 확대" }, onConnect = { event = "폴더 연결" }, onRetry = { event = "이미지 다시 시도" }) {
                        Box(Modifier.fillMaxWidth().height(140.dp).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) { Text(CatalogSamples.markdownAlt) }
                    }
                    MDiveMarkdownText(body)
                }
                "rendered" -> {
                    val formula = buildAnnotatedString { append("인라인 수식 "); appendInlineContent("math", CatalogSamples.markdownFormula); append(" 뒤에도 본문이 이어집니다.") }
                    MDiveMathInline(formula, mapOf("math" to InlineTextContent(Placeholder(70.sp, 24.sp, PlaceholderVerticalAlign.TextCenter)) { Text(CatalogSamples.markdownFormula) }))
                    listOf(MDiveResourceState.Ready, MDiveResourceState.Loading, MDiveResourceState.Failure).forEach { resource ->
                        MDiveRenderedBlock(resource, "수식 · ${CatalogSamples.markdownFormula}", onViewSource = { event = "수식 원문 보기" }) { Text(CatalogSamples.markdownFormula.repeat(12), softWrap = false, style = mdiveTextStyle("body")) }
                    }
                    MDiveRenderedBlock(MDiveResourceState.Ready, "다이어그램: 입력에서 출력으로", onViewSource = { event = "다이어그램 원문 보기" }, onExpand = { event = "다이어그램 확대" }) { Text("[입력] → [처리] → [출력]", Modifier.padding(16.dp)) }
                }
                "fallback" -> { MDiveMarkdownText(body); MDiveContentFallback("부분 콘텐츠 시연", CatalogSamples.markdownReason, onViewSource = { event = "부분 콘텐츠 원문 보기" }); MDiveMarkdownText(body) }
            }
        }
        Text(event, Modifier.testTag("markdown-event"), style = mdiveTextStyle("caption"))
        Text("시각 검토용 데이터입니다. 문법 지원·크기 조절·셀 너비·링크 처리 정책을 확정하지 않습니다.", style = mdiveTextStyle("caption"))
    }
}
