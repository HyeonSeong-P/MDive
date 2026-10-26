package com.parka.mdive.catalog

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<CatalogActivity>()

    @Test
    fun componentGuideThemeSearchAndWideContent() {
        setLight()
        openEntry("search")
        compose.onNodeWithText("1/3").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("0/0").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithContentDescription("이전 검색 결과")[0].assertIsNotEnabled()
        compose.onAllNodesWithContentDescription("이전 검색 결과")[1].assertIsEnabled()
        compose.onAllNodesWithContentDescription("이전 검색 결과")[2].assertIsNotEnabled()
        val query = compose.onNodeWithText("함수").fetchSemanticsNode().boundsInRoot
        val count = compose.onNodeWithText("1/3").fetchSemanticsNode().boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        assertTrue("Search count stays beside the input", kotlin.math.abs(query.center.y - count.center.y) < 20f * density)
        assertTrue("Search count follows input", count.left >= query.right - 1f)
        assertSystemBars(light = true)
        compose.onNodeWithTag("theme-toggle").performClick()
        assertSystemBars(light = false)

        openEntry("content")
        val horizontal = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
        val code = compose.onNode(horizontal and hasText("fun dive", substring = true))
        code.performScrollTo()
        assertTrue(code.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].maxValue() > 0f)
        code.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertTrue(code.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value() > 0f)
        val table = compose.onNode(horizontal and hasAnyDescendant(hasText("구분")))
        table.performScrollTo()
        assertTrue(table.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].maxValue() > 0f)
        table.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertTrue(table.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value() > 0f)
    }

    @Test
    fun paletteSwitchPreservesSelectedComponentAndMode() {
        setLight()
        openEntry("buttons")
        listOf("blue", "green").forEach { palette ->
            selectPalette(palette)
            assertSelection(palette, dark = false, entry = "buttons")
            compose.onNodeWithTag("theme-toggle").performClick()
            assertSelection(palette, dark = true, entry = "buttons")
            selectPalette("deepsea")
            assertSelection("deepsea", dark = true, entry = "buttons")
            compose.onNodeWithTag("theme-toggle").performClick()
        }
        compose.onNodeWithTag("button-primary-disabled").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("button-primary-loading").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun componentNavigationTabsExposeSelectedState() {
        openEntry("navigation")
        compose.onNodeWithTag("sample-tab-toc").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("sample-tab-bookmarks").performClick().assertIsSelected()
    }

    @Test
    fun systemBackReturnsFromDetailToCatalogWithoutFinishingHost() {
        openEntry("buttons")
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithText("MDIVE DESIGN SYSTEM").assertIsDisplayed()
        compose.runOnUiThread {
            assertEquals(null, viewModel().uiState.value.selectedEntryId)
            assertFalse(compose.activity.isFinishing)
            assertFalse(compose.activity.isDestroyed)
        }
    }

    @Test
    fun typographyGuideRendersEveryDefinedRole() {
        openEntry("typography")
        listOf("title", "section", "uiTitle", "body", "bodySmall", "ui", "label", "caption", "code").forEach { role ->
            compose.onNodeWithTag("typography-$role").performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun approvedBrandMarkIsAvailableAcrossCatalogThemes() {
        setLight()
        openEntry("icons")
        compose.onNodeWithTag("brand-mark-themed").performScrollTo().assertIsDisplayed()
        selectPalette("blue")
        assertSelection("blue", dark = false, entry = "icons")
        compose.onNodeWithTag("brand-mark-themed").assertIsDisplayed()
        compose.onNodeWithTag("theme-toggle").performClick()
        assertSelection("blue", dark = true, entry = "icons")
        compose.onNodeWithTag("brand-mark-themed").assertIsDisplayed()
    }

    private fun setLight() {
        compose.runOnUiThread { viewModel().onIntent(CatalogIntent.ToggleTheme(currentDark = true)) }
    }

    private fun openEntry(id: String) {
        compose.runOnUiThread { if (viewModel().uiState.value.selectedEntryId != null) viewModel().onIntent(CatalogIntent.SelectEntry(null)) }
        compose.onNodeWithTag("entry-$id").performScrollTo().performClick()
        compose.onNodeWithTag("detail-$id").assertIsDisplayed()
    }

    private fun selectPalette(palette: String) {
        compose.onNodeWithTag("review-toggle").performClick()
        compose.onNodeWithTag("palette-$palette").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("review-toggle").performClick()
        compose.waitForIdle()
    }

    private fun assertSelection(palette: String, dark: Boolean, entry: String) {
        compose.runOnUiThread {
            assertEquals(palette, viewModel().uiState.value.paletteId)
            assertEquals(dark, viewModel().uiState.value.darkOverride)
            assertEquals(entry, viewModel().uiState.value.selectedEntryId)
        }
    }

    private fun viewModel() = androidx.lifecycle.ViewModelProvider(compose.activity)[CatalogViewModel::class.java]

    private fun assertSystemBars(light: Boolean) {
        compose.waitForIdle()
        compose.runOnUiThread {
            val controller = WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView)
            assertEquals(light, controller.isAppearanceLightStatusBars)
            assertEquals(light, controller.isAppearanceLightNavigationBars)
        }
    }
}
