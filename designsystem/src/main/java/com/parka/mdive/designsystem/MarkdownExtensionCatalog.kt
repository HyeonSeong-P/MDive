package com.parka.mdive.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.generated.CatalogSamples

@Composable
internal fun MarkdownExtensionCatalog(state: String) {
    var event by remember(state) { mutableStateOf("이벤트 대기 · 호스트 처리 시연") }
    val navigate: (String) -> Unit = { event = "이동 대상: $it" }
    val note = listOf(MDiveTextRun(CatalogSamples.extensionNote))
    val references = CatalogSamples.extensionReferenceLinks.map { MDiveDocumentReference(it[0], it[1]) }
    MDiveMarkdownFlow {
        when (state) {
            "inline" -> MDiveMarkdownText(listOf(MDiveTextRun("문법 highlight", mark = true), MDiveTextRun(" · 검색 강조", highlighted = true), MDiveTextRun(" · H"), MDiveTextRun("2", baseline = MDiveTextBaseline.Subscript), MDiveTextRun("O · x"), MDiveTextRun("2", baseline = MDiveTextBaseline.Superscript), MDiveTextRun(" · 삽입", inserted = true), MDiveTextRun(" · 밑줄", underline = true), MDiveTextRun(" · 중첩 강조", mark = true, strong = true, emphasis = true)))
            "references" -> {
                MDiveMarkdownText(note)
                MDiveMarkdownReference(references[0], navigate, superscript = true)
                MDiveMarkdownReference(references[1], navigate)
                MDiveReferenceList(
                    entries = listOf(
                        MDiveReferenceEntry(MDiveDocumentReference("1.", CatalogSamples.extensionTarget), note, listOf(references[2])),
                        MDiveReferenceEntry(references[1], listOf(MDiveTextRun(CatalogSamples.extensionReference)), listOf(references[3])),
                    ),
                    onNavigate = navigate,
                )
            }
            "definitions" -> MDiveDefinitionList(listOf(MDiveDefinition(listOf(MDiveTextRun(CatalogSamples.extensionTerm)), listOf(listOf(MDiveTextRun(CatalogSamples.extensionDefinition)), note))))
            "callouts" -> {
                MDiveMarkdownCallout("일반 보조 설명") { MDiveMarkdownText(note) }
                MDiveCalloutKind.entries.forEach { kind -> MDiveMarkdownCallout(kind.name, kind = kind) { MDiveMarkdownText(note); MDiveMarkdownListItem(MDiveListMarker.Bullet) { MDiveMarkdownText(note) } } }
            }
            "details" -> {
                var expanded by remember { mutableStateOf(false) }
                MDiveMarkdownDetails("추가 설명", expanded, { expanded = it; event = "펼침: $it" }, summaryContent = { MDiveMarkdownText(listOf(MDiveTextRun("추가 설명 · ", strong = true), MDiveTextRun("누르면 펼칩니다"))) }) { MDiveMarkdownText(note); MDiveCodeBlock(CatalogSamples.code) }
            }
            "metadata" -> MDiveMarkdownMetadata(CatalogSamples.extensionMetadata.map { MDiveMetadataField(it[0], listOf(MDiveTextRun(it[1]))) })
            "toc" -> MDiveMarkdownToc(CatalogSamples.extensionTocTargets.map { MDiveTocEntry(MDiveDocumentReference(it[0], it[1]), it[2].toInt()) }, navigate)
            "embedded" -> {
                MDiveMarkdownText(note)
                listOf(MDiveResourceState.Ready, MDiveResourceState.Loading, MDiveResourceState.Failure).forEach { resource ->
                    MDiveEmbeddedContent(resource, "준비된 HTML 시각 샘플", onViewSource = { event = "HTML 원문 보기" }) { Text("<section>준비된 텍스트</section>", Modifier.padding(16.dp)) }
                    MDiveEmbeddedContent(resource, "미디어 시각 slot · 실행 엔진 없음", onViewSource = { event = "미디어 원문 보기" }, onExpand = { event = "미디어 확대" }) { Box(Modifier.width(240.dp).height(100.dp).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) { Text("준비된 미디어 프레임") } }
                }
                MDiveMarkdownText(note)
            }
            "resolved" -> {
                MDiveMarkdownHeading(3, listOf(MDiveTextRun("사용자 ID가 연결된 제목")))
                MDiveMarkdownText(CatalogSamples.extensionResolvedLinks.flatMapIndexed { index, row -> (if (index == 0) emptyList() else listOf(MDiveTextRun(" · "))) + MDiveTextRun(row[0], link = row[1]) }, onLink = navigate)
                MDiveMarkdownText(listOf(MDiveTextRun("해석된 emoji 🌊 · typography “읽기” — 다음 문장…")))
                Text("주석의 비노출·치환·ID/속성/참조 해석은 비시각 단계입니다. 이 예시는 파싱하지 않습니다.", style = mdiveTextStyle("caption"))
            }
        }
        Text(event, style = mdiveTextStyle("caption"))
        Text("표시용 데이터와 콜백만 사용합니다. 방언·정책·초기 펼침 상태·네트워크·HTML 실행은 결정하지 않습니다.", style = mdiveTextStyle("caption"))
    }
}
