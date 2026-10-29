package com.parka.mdive.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.parka.mdive.designsystem.generated.DesignTokens

private val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
)
private val JetBrainsMono = FontFamily(Font(R.font.jetbrains_mono_regular))
private fun fontFamilyForRole(role: String): FontFamily = when (val family = DesignTokens.FontRoles.getValue(role)) {
    "Pretendard" -> Pretendard
    "JetBrains Mono" -> JetBrainsMono
    else -> error("Unregistered font family: $family")
}
/** Resolve a registered typography role for host-provided prepared content slots. */
fun mdiveTextStyle(role: String): TextStyle {
    val token = DesignTokens.Typography.getValue(role)
    return TextStyle(
        fontFamily = fontFamilyForRole(token.familyRole),
        fontSize = token.fontSize,
        lineHeight = token.lineHeight,
        fontWeight = FontWeight(token.weight),
    )
}
internal val LocalOceanColors = staticCompositionLocalOf { DesignTokens.Light }

@Composable
fun MDiveDesignTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    paletteId: String = "deepsea",
    content: @Composable () -> Unit,
) {
    val colors = (if (darkTheme) DesignTokens.DarkThemes else DesignTokens.LightThemes).getValue(paletteId)
    val scheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalOceanColors provides colors) {
        MaterialTheme(
            colorScheme = scheme.copy(
                background = colors.getValue("background"),
                surface = colors.getValue("surface"),
                surfaceVariant = colors.getValue("surfaceVariant"),
                onBackground = colors.getValue("onBackground"),
                onSurface = colors.getValue("onSurface"),
                onSurfaceVariant = colors.getValue("muted"),
                primary = colors.getValue("primary"),
                onPrimary = colors.getValue("onPrimary"),
                outline = colors.getValue("outline"),
                outlineVariant = colors.getValue("outline"),
                surfaceTint = colors.getValue("primary"),
                primaryContainer = colors.getValue("surfaceVariant"),
                onPrimaryContainer = colors.getValue("onSurface"),
                secondary = colors.getValue("primary"),
                onSecondary = colors.getValue("onPrimary"),
                secondaryContainer = colors.getValue("surfaceVariant"),
                onSecondaryContainer = colors.getValue("onSurface"),
                tertiary = colors.getValue("primary"),
                onTertiary = colors.getValue("onPrimary"),
                tertiaryContainer = colors.getValue("surfaceVariant"),
                onTertiaryContainer = colors.getValue("onSurface"),
                inverseSurface = colors.getValue("onSurface"),
                inverseOnSurface = colors.getValue("surface"),
                inversePrimary = colors.getValue("onPrimary"),
                surfaceDim = colors.getValue("background"),
                surfaceBright = colors.getValue("surface"),
                surfaceContainerLowest = colors.getValue("background"),
                surfaceContainerLow = colors.getValue("surface"),
                surfaceContainer = colors.getValue("surface"),
                surfaceContainerHigh = colors.getValue("surfaceVariant"),
                surfaceContainerHighest = colors.getValue("surfaceVariant"),
                onError = colors.getValue("surface"),
                errorContainer = colors.getValue("surfaceVariant"),
                onErrorContainer = colors.getValue("error"),
                scrim = colors.getValue("onBackground"),
                error = colors.getValue("error"),
            ),
            typography = Typography(
                displayLarge = mdiveTextStyle("title"),
                displayMedium = mdiveTextStyle("title"),
                displaySmall = mdiveTextStyle("title"),
                headlineLarge = mdiveTextStyle("title"),
                headlineMedium = mdiveTextStyle("title"),
                headlineSmall = mdiveTextStyle("section"),
                titleLarge = mdiveTextStyle("title"),
                titleMedium = mdiveTextStyle("uiTitle"),
                titleSmall = mdiveTextStyle("uiTitle"),
                bodyLarge = mdiveTextStyle("body"),
                bodyMedium = mdiveTextStyle("ui"),
                bodySmall = mdiveTextStyle("bodySmall"),
                labelLarge = mdiveTextStyle("label"),
                labelMedium = mdiveTextStyle("label"),
                labelSmall = mdiveTextStyle("caption"),
            ),
            content = content,
        )
    }
}
