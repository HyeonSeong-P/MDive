package com.parka.mdive.data.document

import android.graphics.Bitmap
import android.graphics.Canvas
import com.parka.mdive.domain.document.MathRaster
import javax.inject.Inject
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import ru.noties.jlatexmath.awt.AndroidGraphics2D
import ru.noties.jlatexmath.awt.Color

/** Native engine calls are serialized because TeXFormula owns mutable global registries. */
class MathRenderer @Inject constructor() {
    fun render(tex: String, display: Boolean): MathRaster? {
        if (!MathProfile.accepts(tex)) return null
        return synchronized(lock) {
            try {
                val icon = TeXFormula(tex).TeXIconBuilder()
                    .setFGColor(Color(android.graphics.Color.BLACK)).setSize(64f)
                    .setStyle(if (display) TeXConstants.STYLE_DISPLAY else TeXConstants.STYLE_TEXT).build()
                val width = icon.iconWidth
                val height = icon.iconHeight
                if (width !in 1..16384 || height !in 1..4096 || width.toLong() * height > 2_000_000) return@synchronized null
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                try {
                    val graphics = AndroidGraphics2D()
                    graphics.setCanvas(Canvas(bitmap))
                    icon.paintIcon(null, graphics, 0, 0)
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    MathRaster(width, height, icon.iconDepth, pixels)
                } finally { bitmap.recycle() }
            } catch (_: Exception) { null } catch (_: LinkageError) { null } catch (_: StackOverflowError) { null }
        }
    }
    private companion object { val lock = Any() }
}

/** Deliberately finite profile: no macro definitions, colors, URLs, files, or engine extensions. */
internal object MathProfile {
    val commands = ("frac dfrac tfrac sqrt left right middle over under atop choose binom " +
        "sum prod coprod int iint iiint oint lim min max inf sup log ln exp sin cos tan cot sec csc " +
        "sinh cosh tanh arcsin arccos arctan det gcd deg dim ker hom Pr " +
        "alpha beta gamma delta epsilon varepsilon zeta eta theta vartheta iota kappa lambda mu nu xi pi varpi rho varrho sigma varsigma tau upsilon phi varphi chi psi omega " +
        "Gamma Delta Theta Lambda Xi Pi Sigma Upsilon Phi Psi Omega " +
        "infty partial nabla ell hbar imath jmath Re Im emptyset forall exists neg " +
        "times cdot div pm mp ast star circ bullet cap cup setminus wedge vee oplus otimes " +
        "le leq ge geq ne neq approx sim simeq equiv propto in notin ni subset supset subseteq supseteq " +
        "to gets rightarrow leftarrow leftrightarrow Rightarrow Leftarrow Leftrightarrow mapsto " +
        "ldots cdots vdots ddots dots angle triangle perp parallel " +
        "hat widehat bar overline underline vec dot ddot tilde widetilde " +
        "text textrm textbf textit mathrm mathbf mathit mathsf mathtt mathcal mathbb mathnormal " +
        "limits nolimits displaystyle textstyle scriptstyle scriptscriptstyle begin end").split(' ').toSet()
    private val environments = setOf("matrix", "pmatrix", "bmatrix", "Bmatrix", "vmatrix", "Vmatrix", "cases", "aligned", "gathered", "smallmatrix", "array")
    fun accepts(tex: String): Boolean {
        if (tex.length > 4096) return false
        var depth = 0
        var i = 0
        var commandsSeen = 0
        var separators = 0
        while (i < tex.length) {
            when (tex[i]) {
                '&' -> { separators++; if (separators > 128) return false }
                '{' -> { depth++; if (depth > 32) return false }
                '}' -> { depth--; if (depth < 0) return false }
                '\\' -> {
                    i++
                    if (i >= tex.length) return false
                    if (tex[i].isLetter()) {
                        val start = i
                        while (i < tex.length && tex[i].isLetter()) i++
                        val command = tex.substring(start, i)
                        if (++commandsSeen > 256) return false
                        if (command !in commands) return false
                        if (command == "begin" || command == "end") {
                            val end = tex.indexOf('}', i)
                            if (tex.getOrNull(i) != '{' || end < 0 || tex.substring(i + 1, end) !in environments) return false
                            if (command == "begin" && tex.substring(i + 1, end) == "array") {
                                var columnStart = end + 1
                                while (columnStart < tex.length && tex[columnStart].isWhitespace()) columnStart++
                                if (tex.getOrNull(columnStart) != '{') return false
                                val columnEnd = tex.indexOf('}', columnStart + 1)
                                if (columnEnd < 0) return false
                                val columns = tex.substring(columnStart + 1, columnEnd)
                                if (columns.length > 64 || columns.any { it !in "lcr| " }) return false
                            }
                        }
                        continue
                    } else if (tex[i] == '\\') {
                        separators++; if (separators > 128) return false
                    } else if (tex[i] !in "\\{}$%#&_ ,;:!|>") return false
                }
            }
            i++
        }
        return depth == 0
    }
}
