package com.parka.mdive.data.document

/** A deliberately restricted, non-executing input gate; Mermaid still validates its grammar. */
internal object MermaidProfile {
    fun accepts(code: String): Boolean {
        if (code.isBlank() || code.length > 50_000) return false
        // Reject directives/config before comments are removed, including disguised front matter.
        if (code.contains("%%{") || code.trimStart().startsWith("---")) return false
        val text = code.lineSequence().filterNot { it.trimStart().startsWith("%%") }.joinToString("\n")
        if (!Regex("^(?:flowchart|graph)\\s+(?:TB|TD|BT|RL|LR)\\b|^(?:sequenceDiagram|classDiagram|stateDiagram-v2|erDiagram)\\b").containsMatchIn(text.trimStart())) return false
        // No user CSS, HTML, callbacks, links, resource shapes or encoded markup. These are
        // unsupported inputs, not strings we attempt to sanitize into different diagrams.
        if (Regex("(?im)(?:^|;)\\s*(?:click|link|links|style|classDef|linkStyle)\\b").containsMatchIn(text)) return false
        // Mermaid stereotypes (for example <<choice>> and <<interface>>) are native
        // diagram notation, not HTML tags.
        val markup = text.replace(Regex("(?<!<)<<(?:choice|fork|join|interface|abstract|service|enumeration)>>(?!>)"), "")
        if (Regex("(?i)<[!/?a-z]|&(?:lt|gt|#)\\w*;|url\\s*\\(|(?:https?|file|content|javascript|data):|@\\{|(?:href|callback)\\s*[:=]").containsMatchIn(markup)) return false
        return true
    }
}
