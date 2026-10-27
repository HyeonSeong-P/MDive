package com.parka.mdive.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parka.mdive.designsystem.generated.DesignTokens

/** Resolved destinations are opaque values; the host decides how to navigate. */
data class MDiveDocumentReference(val label: String, val target: String)
data class MDiveReferenceEntry(val reference: MDiveDocumentReference, val content: List<MDiveTextRun>, val backlinks: List<MDiveDocumentReference> = emptyList())

@Composable
fun MDiveMarkdownReference(reference: MDiveDocumentReference, onNavigate: (String) -> Unit, modifier: Modifier = Modifier, superscript: Boolean = false) {
    MDiveMarkdownText(listOf(MDiveTextRun(reference.label, link = reference.target, baseline = if (superscript) MDiveTextBaseline.Superscript else MDiveTextBaseline.Normal)), modifier, onLink = onNavigate)
}

/** Used by both footnotes and bibliography; callers provide resolved labels and return links. */
@Composable
fun MDiveReferenceList(entries: List<MDiveReferenceEntry>, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    MDiveMarkdownFlow(modifier) {
        entries.forEach { entry ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MDiveMarkdownText(listOf(MDiveTextRun(entry.reference.label, strong = true)) + listOf(MDiveTextRun(" ")) + entry.content, onLink = onNavigate)
                entry.backlinks.forEach { MDiveMarkdownReference(it, onNavigate) }
            }
        }
    }
}

data class MDiveDefinition(val term: List<MDiveTextRun>, val meanings: List<List<MDiveTextRun>>)

@Composable
fun MDiveDefinitionList(definitions: List<MDiveDefinition>, modifier: Modifier = Modifier, onLink: ((String) -> Unit)? = null) {
    MDiveMarkdownFlow(modifier) {
        definitions.forEach { definition ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MDiveMarkdownText(definition.term.map { it.copy(strong = true) }, onLink = onLink)
                definition.meanings.forEach { MDiveMarkdownText(it, Modifier.padding(start = DesignTokens.Spacing.getValue("documentInset")), onLink) }
            }
        }
    }
}

enum class MDiveCalloutKind(val token: String, val icon: String) {
    Note("Info", "file"), Tip("Success", "check"), Important("Important", "star"), Warning("Warning", "warning"), Caution("Danger", "warning"),
}

/** A generic content container; title/content are independent of any dialect or parser. */
@Composable
fun MDiveMarkdownCallout(title: String, modifier: Modifier = Modifier, kind: MDiveCalloutKind = MDiveCalloutKind.Note, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalOceanColors.current
    val shape = RoundedCornerShape(DesignTokens.Radii.getValue("panel"))
    Column(modifier.fillMaxWidth().background(colors.getValue("callout${kind.token}Background"), shape)
        .border(1.dp, colors.getValue("callout${kind.token}"), shape).padding(DesignTokens.Spacing.getValue("md")), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.getValue("sm"))) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MDiveIcon(kind.icon, tint = colors.getValue("callout${kind.token}"), size = 20.dp)
            Text(title, Modifier.weight(1f).semantics { heading() }, color = colors.getValue("callout${kind.token}"), style = mdiveTextStyle("uiTitle"))
        }
        content()
    }
}

/** Controlled expansion: initial state, persistence and hidden-content policies belong to the host. */
@Composable
fun MDiveMarkdownDetails(summary: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, summaryContent: @Composable () -> Unit = { Text(summary, style = mdiveTextStyle("uiTitle")) }, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onExpandedChange(!expanded) }.semantics { stateDescription = if (expanded) "펼쳐짐" else "접힘" }.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MDiveIcon(if (expanded) "up" else "down")
            Box(Modifier.weight(1f)) { summaryContent() }
        }
        if (expanded) Column(Modifier.fillMaxWidth().padding(start = DesignTokens.Spacing.getValue("documentInset")), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

data class MDiveMetadataField(val label: String, val value: List<MDiveTextRun>)

/** Display-only metadata supplied by the host, never YAML/TOML parsing or field selection. */
@Composable
fun MDiveMarkdownMetadata(fields: List<MDiveMetadataField>, modifier: Modifier = Modifier, onLink: ((String) -> Unit)? = null) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        fields.forEach { field ->
            Column {
                Text(field.label, style = mdiveTextStyle("label"))
                MDiveMarkdownText(field.value, onLink = onLink, style = mdiveTextStyle("ui"))
            }
        }
    }
}

data class MDiveTocEntry(val reference: MDiveDocumentReference, val level: Int = 1) {
    init { require(level in 1..6) { "TOC level must be 1..6" } }
}

@Composable
fun MDiveMarkdownToc(entries: List<MDiveTocEntry>, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().semantics { collectionInfo = CollectionInfo(entries.size, 1) }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEachIndexed { index, entry ->
            MDiveMarkdownReference(entry.reference, onNavigate, Modifier.padding(start = DesignTokens.Spacing.getValue("documentInset") * (entry.level - 1)).semantics { collectionItemInfo = CollectionItemInfo(index, 1, 0, 1) })
        }
    }
}

/** Prepared HTML/media presentation only: no URL, browser, decoder or executable content API. */
@Composable
fun MDiveEmbeddedContent(state: MDiveResourceState, description: String, modifier: Modifier = Modifier, onViewSource: (() -> Unit)? = null, onExpand: (() -> Unit)? = null, content: @Composable () -> Unit) {
    MDiveRenderedBlock(state, description, modifier, onViewSource, onExpand, content)
}
