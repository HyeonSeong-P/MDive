package com.parka.mdive.data.document

import com.parka.mdive.domain.document.CodeHighlight
import com.parka.mdive.domain.document.CodeTone
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxLanguage
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/** Engine types and its approximate lexical analysis stay inside data. */
class CodeHighlighter @Inject constructor() {
    internal fun highlight(source: String, language: String?): List<CodeHighlight> =
        safelyHighlight(source) {
            val syntax = languages[language?.lowercase(Locale.ROOT)] ?: return@safelyHighlight emptyList()
            val structure = Highlights.Builder().code(source).language(syntax).build().getCodeStructure()
            buildList {
                structure.keywords.forEach { add(CodeHighlight(it.start, it.end, CodeTone.Keyword)) }
                structure.strings.forEach { add(CodeHighlight(it.start, it.end, CodeTone.String)) }
                // Comments override any overlapping lexical hints, using the existing neutral DS tone.
                (structure.comments + structure.multilineComments).forEach { add(CodeHighlight(it.start, it.end, CodeTone.Plain)) }
            }
        }

    private companion object {
        val languages = buildMap {
            fun language(value: SyntaxLanguage, vararg names: String) { names.forEach { put(it, value) } }
            language(SyntaxLanguage.C, "c")
            language(SyntaxLanguage.CPP, "cpp", "c++", "cxx")
            language(SyntaxLanguage.DART, "dart")
            language(SyntaxLanguage.JAVA, "java")
            language(SyntaxLanguage.KOTLIN, "kotlin", "kt", "kts")
            language(SyntaxLanguage.RUST, "rust", "rs")
            language(SyntaxLanguage.CSHARP, "csharp", "c#", "cs")
            language(SyntaxLanguage.COFFEESCRIPT, "coffeescript", "coffee")
            language(SyntaxLanguage.JAVASCRIPT, "javascript", "js")
            language(SyntaxLanguage.PERL, "perl", "pl")
            language(SyntaxLanguage.PYTHON, "python", "py")
            language(SyntaxLanguage.RUBY, "ruby", "rb")
            language(SyntaxLanguage.SHELL, "shell", "bash", "sh", "zsh")
            language(SyntaxLanguage.SWIFT, "swift")
            language(SyntaxLanguage.TYPESCRIPT, "typescript", "ts")
            language(SyntaxLanguage.GO, "go", "golang")
            language(SyntaxLanguage.PHP, "php")
        }
    }
}

/** A broken engine result only removes color, never source or surrounding content. */
internal inline fun safelyHighlight(source: String, analyze: () -> List<CodeHighlight>): List<CodeHighlight> = try {
    analyze().takeIf { ranges -> ranges.all { it.start >= 0 && it.start < it.end && it.end <= source.length } }.orEmpty()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    emptyList()
}
