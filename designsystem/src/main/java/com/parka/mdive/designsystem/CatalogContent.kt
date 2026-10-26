package com.parka.mdive.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parka.mdive.designsystem.generated.CatalogEntry
import com.parka.mdive.designsystem.generated.CatalogIcons
import com.parka.mdive.designsystem.generated.CatalogSamples
import com.parka.mdive.designsystem.generated.CatalogThemes
import com.parka.mdive.designsystem.generated.DesignTokens
import com.parka.mdive.designsystem.generated.catalogEntries

/** A component guide. Review controls never imply product theme settings. */
@Composable
fun CatalogContent(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    onShowLicenses: () -> Unit = {},
    paletteId: String = "deepsea",
    onSelectPalette: (String) -> Unit = {},
    reviewExpanded: Boolean = false,
    onToggleReview: () -> Unit = {},
    selectedEntryId: String? = null,
    onSelectEntry: (String?) -> Unit = {},
) {
    val selected = selectedEntryId?.let { id -> catalogEntries.first { it.id == id } }
    MDiveDesignTheme(darkTheme = darkTheme, paletteId = paletteId) {
        Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                MDiveTopBar(
                    title = "MDive / 디자인 시스템",
                    subtitle = "${CatalogThemes.names.getValue(paletteId)} · ${if (darkTheme) "다크" else "라이트"} · 초안",
                    navigationIcon = if (selected == null) null else "back",
                    navigationDescription = if (selected == null) null else "컴포넌트 목록",
                    onNavigationClick = { onSelectEntry(null) },
                    leading = if (selected == null) ({ MDiveBrandMark(Modifier.size(36.dp, 28.dp), contentDescription = null) }) else null,
                    actions = {
                        MDiveIconButton(if (darkTheme) "sun" else "moon", if (darkTheme) "라이트 보기" else "다크 보기", onToggleTheme, Modifier.testTag("theme-toggle"))
                        MDiveIconButton(if (reviewExpanded) "close" else "settings", "검토 설정", onToggleReview, Modifier.testTag("review-toggle"))
                    },
                )
                if (reviewExpanded) {
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp)) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            CatalogThemes.names.forEach { (id, label) ->
                                MDiveTextTab(label, paletteId == id, { onSelectPalette(id) }, Modifier.testTag("palette-$id"))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("카탈로그 비교용 · 제품 설정 아님", Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            MDiveButton("라이선스", onShowLicenses, variant = MDiveButtonVariant.Quiet)
                        }
                    }
                }
                MDiveDivider()
                if (selected == null) CatalogIndex(onSelectEntry, Modifier.weight(1f))
                else CatalogDetail(selected, darkTheme, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CatalogIndex(onSelectEntry: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 32.dp, bottom = 40.dp)) {
        Eyebrow("MDIVE DESIGN SYSTEM")
        Spacer(Modifier.height(12.dp))
        Text("읽기에 집중하는\n작은 규칙들.", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text("기초 규칙과 컴포넌트의 용도·변형·상태를 확인합니다. 제품 화면에 적용하기 전 검토하는 카탈로그입니다.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf("foundations" to "Foundations", "components" to "Components").forEach { (category, label) ->
            Spacer(Modifier.height(32.dp))
            Eyebrow(label.uppercase())
            Spacer(Modifier.height(8.dp))
            catalogEntries.filter { it.category == category }.forEach { entry -> CatalogEntryLink(entry, { onSelectEntry(entry.id) }) }
        }
    }
}

@Composable
private fun CatalogEntryLink(entry: CatalogEntry, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    Column {
        Row(
            Modifier.fillMaxWidth().testTag("entry-${entry.id}")
                .background(if (pressed || focused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick).padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium)
                Text(entry.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MDiveIcon("chevron", tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 20.dp)
        }
        MDiveDivider()
    }
}

@Composable
private fun CatalogDetail(entry: CatalogEntry, darkTheme: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 32.dp, bottom = 40.dp).testTag("detail-${entry.id}"), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Eyebrow("${entry.category.uppercase()} / ${entry.id.uppercase()}")
        Text(entry.title, style = MaterialTheme.typography.titleLarge)
        Text(entry.purpose, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GuideSection("구조", entry.anatomy)
        MDiveDivider()
        Text("변형과 상태", style = mdiveTextStyle("section"))
        if (entry.id == "typography") Text("표시 수치는 시스템 글꼴 배율 1.0의 기준 sp입니다. 실제 글자는 Android 시스템 글꼴 크기에 따라 확대됩니다. 아래 역할 값은 검토 중인 초안입니다.", style = mdiveTextStyle("bodySmall"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (entry.id == "buttons") ButtonMatrix(entry)
        else {
            if (entry.variants.isNotEmpty()) Text(entry.variants.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            entry.states.forEach { state ->
                Text(state.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                CatalogExample(entry, state, darkTheme)
            }
        }
        MDiveDivider()
        GuideSection("동작", entry.behavior)
        GuideSection("접근성", entry.accessibility)
        GuideSection("사용 기준", entry.usage)
    }
}

@Composable
private fun GuideSection(title: String, lines: List<String>) {
    if (lines.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = mdiveTextStyle("section"))
        lines.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ButtonMatrix(entry: CatalogEntry) {
    entry.variants.forEach { variant ->
        Text(variant.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            entry.states.forEach { state ->
                Column(Modifier.width(148.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MDiveButton(
                        if (state == "loading") "처리 중" else "파일 열기", {}, Modifier.fillMaxWidth().testTag("button-$variant-$state"),
                        variant = when (variant) { "primary" -> MDiveButtonVariant.Primary; "secondary" -> MDiveButtonVariant.Secondary; "quiet" -> MDiveButtonVariant.Quiet; else -> error("Unknown button variant: $variant") },
                        state = when (state) { "normal" -> MDiveButtonState.Normal; "pressed" -> MDiveButtonState.Pressed; "disabled" -> MDiveButtonState.Disabled; "loading" -> MDiveButtonState.Loading; else -> error("Unknown button state: $state") },
                        icon = "plus",
                    )
                }
            }
        }
    }
}

@Composable
private fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun CatalogExample(entry: CatalogEntry, state: String, darkTheme: Boolean) {
    when (entry.id) {
        "colors" -> if (state == if (darkTheme) "dark" else "light") LocalOceanColors.current.forEach { (name, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(24.dp).background(color))
                Text(name, style = MaterialTheme.typography.bodyMedium)
            }
        }
        "typography" -> {
            val token = DesignTokens.Typography.getValue(state)
            val sample = when (state) {
                "title" -> CatalogSamples.title
                "section" -> "문서의 흐름을 살피기"
                "uiTitle" -> CatalogSamples.filename
                "body" -> CatalogSamples.body
                "bodySmall" -> "생각을 정리한 문장을 차분히 이어 읽습니다. 한글과 English가 함께 놓여도 읽기의 리듬을 유지합니다."
                "ui" -> "목차 · 책갈피 · 문서 검색"
                "label" -> "파일 열기"
                "caption" -> "최근에 읽은 문서 · 보조 정보"
                "code" -> "val reading = \"MDive\" // 0 O 1 l I"
                else -> error("Unknown typography role: $state")
            }
            Column(Modifier.fillMaxWidth().testTag("typography-$state"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$state · ${DesignTokens.FontRoles.getValue(token.familyRole)} · ${token.fontSize.value}sp · ${token.weight} · 행간 ${token.lineHeight.value}sp", style = mdiveTextStyle("caption"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(sample, style = mdiveTextStyle(state))
            }
        }
        "layout" -> when (state) {
            "spacing" -> DesignTokens.Spacing.forEach { (name, spacing) -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Text("$name / $spacing", Modifier.width(130.dp), style = MaterialTheme.typography.labelSmall); Box(Modifier.width(spacing).height(12.dp).background(MaterialTheme.colorScheme.primary)) } }
            "radius" -> Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) { DesignTokens.Radii.forEach { (name, radius) -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(48.dp).clip(RoundedCornerShape(radius)).background(MaterialTheme.colorScheme.surfaceVariant)); Text("$name / $radius", style = MaterialTheme.typography.labelSmall) } } }
        }
        "documents" -> when (state) {
            "normal" -> MDiveDocumentRow(CatalogSamples.filename, "즐겨찾기 · 예시 문서", {}, favorite = true)
            "empty" -> MDiveNotice("아직 문서가 없어요", "첫 문서를 열어보세요.", "empty")
            "error" -> MDiveNotice("문서를 열지 못했어요", "파일을 다시 선택하거나 목록에서 제거할 수 있어요.", "error")
        }
        "search" -> {
            var query by remember(state) { mutableStateOf(when (state) { "idle" -> ""; "empty" -> "없는 단어"; else -> "함수" }) }
            MDiveSearchBar(query, when (state) { "idle" -> "—"; "empty" -> "0/0"; else -> "1/3" }, { query = it }, {}, {}, {}, navigationEnabled = state == "results")
            Text("입력 체험용 · 결과 건수는 시연 값입니다.", style = mdiveTextStyle("caption"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state == "empty") Text("일치하는 내용을 찾지 못했어요.", style = MaterialTheme.typography.bodyMedium)
        }
        "icons" -> if (state == "brand") {
            Text("MDive · 대표 심볼", style = mdiveTextStyle("uiTitle"))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                MDiveBrandMark(Modifier.testTag("brand-mark-themed"))
                Box(Modifier.clip(RoundedCornerShape(DesignTokens.Radii.getValue("panel"))).background(DesignTokens.Dark.getValue("background")).padding(16.dp)) {
                    MDiveBrandMark(tint = Color.Unspecified)
                }
            }
            Text("상단바에는 테마 색 단색, 짙은 배경에는 브랜드 원색을 사용할 수 있습니다. 제목과 함께 쓸 때는 중복 낭독을 피합니다.", style = mdiveTextStyle("caption"))
        } else CatalogIcons.paths.keys.toList().chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { name -> Column(Modifier.width(64.dp).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { MDiveIcon(name); Text(name, style = MaterialTheme.typography.labelSmall) } }
            }
        }
        "navigation" -> {
            var tab by remember { mutableIntStateOf(0) }
            Row { MDiveTextTab("목차", tab == 0, { tab = 0 }, Modifier.testTag("sample-tab-toc")); MDiveTextTab("책갈피", tab == 1, { tab = 1 }, Modifier.testTag("sample-tab-bookmarks")) }
        }
        "feedback" -> when (state) {
            "loading" -> MDiveNotice("문서를 준비하고 있어요", "본문을 읽을 준비가 되면 표시합니다.", state)
            "empty" -> MDiveNotice("아직 책갈피가 없어요", "다시 읽고 싶은 위치를 저장해보세요.", state)
            "error" -> MDiveNotice("문서를 열지 못했어요", "기존 목록은 유지됩니다. 다시 선택해주세요.", state)
        }
        "topbar" -> {
            val title = when (state) {
                "default" -> "MDive"
                "longTitle" -> CatalogSamples.longFilename
                else -> CatalogSamples.filename
            }
            MDiveTopBar(
                title = title,
                navigationIcon = if (state == "default") null else "back",
                navigationDescription = if (state == "default") null else "뒤로 가기 · 시안",
                actions = {
                    if (state == "actions" || state == "longTitle") {
                        MDiveIconButton("search", "문서 검색 · 시안", {})
                        MDiveIconButton("star", "즐겨찾기 · 시안", {})
                    }
                },
                modifier = Modifier.testTag("topbar-$state"),
            )
            MDiveDivider()
        }
        "content" -> when (state) { "code" -> MDiveCodeSample(); "table" -> MDiveTableSample() }
        else -> error("Missing catalog renderer: ${entry.id}")
    }
}

@Preview(name = "카탈로그 목록 · 라이트", showBackground = true)
@Composable
private fun LightCatalogPreview() { CatalogContent(false, {}) }

@Preview(name = "버튼 · 다크", showBackground = true)
@Composable
private fun DarkCatalogPreview() { CatalogContent(true, {}, selectedEntryId = "buttons") }
