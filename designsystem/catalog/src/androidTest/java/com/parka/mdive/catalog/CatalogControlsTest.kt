package com.parka.mdive.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parka.mdive.designsystem.MDiveButton
import com.parka.mdive.designsystem.MDiveBrandMark
import com.parka.mdive.designsystem.MDiveButtonState
import com.parka.mdive.designsystem.MDiveDesignTheme
import com.parka.mdive.designsystem.MDiveSearchBar
import com.parka.mdive.designsystem.MDiveTopBar
import com.parka.mdive.designsystem.MDiveIconButton
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun normalButtonDispatchesWhileDisabledAndLoadingDoNot() {
        val clicks = AtomicInteger()
        compose.setContent {
            MDiveDesignTheme {
                Column {
                    MDiveButton("열기", { clicks.incrementAndGet() }, Modifier.testTag("normal"))
                    MDiveButton("비활성", { clicks.incrementAndGet() }, Modifier.testTag("disabled"), state = MDiveButtonState.Disabled)
                    MDiveButton("준비 중", { clicks.incrementAndGet() }, Modifier.testTag("loading"), state = MDiveButtonState.Loading)
                }
            }
        }
        compose.onNodeWithTag("normal").performClick()
        compose.onNodeWithTag("disabled").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithTag("loading").assertIsNotEnabled().performTouchInput { click() }
        assertEquals(1, clicks.get())
    }

    @Test
    fun searchInputAcceptsFocusAndTextInNarrowBar() {
        compose.setContent {
            MDiveDesignTheme {
                var query by remember { mutableStateOf("") }
                Box(Modifier.width(320.dp)) {
                    MDiveSearchBar(query, "1/3", { query = it }, {}, {}, {})
                }
            }
        }
        compose.onNodeWithText("문서 검색").assertIsDisplayed()
        compose.onNodeWithContentDescription("문서 내 검색").performClick().assertIsFocused().performTextInput("바닷속")
        compose.onNodeWithContentDescription("문서 내 검색").assertTextEquals("바닷속")
        compose.onNodeWithText("1/3").assertIsDisplayed()
        compose.onNodeWithContentDescription("이전 검색 결과").assertIsDisplayed()
        compose.onNodeWithContentDescription("다음 검색 결과").assertIsDisplayed()
    }

    @Test
    fun brandOnlyTopBarExposesBrandWithoutTitle() {
        compose.setContent { MDiveDesignTheme { MDiveTopBar(leading = { MDiveBrandMark() }) } }
        compose.onNodeWithContentDescription("MDive").assertIsDisplayed()
        compose.onNodeWithText("MDive").assertDoesNotExist()
    }

    @Test
    fun longTopBarTitleKeepsNamedActionsAvailable() {
        val actions = AtomicInteger()
        val backs = AtomicInteger()
        val title = "매우 긴 제목의 Markdown 문서에서 상단 동작을 확인하는 예시.md"
        compose.setContent {
            MDiveDesignTheme {
                Box(Modifier.width(320.dp)) {
                    MDiveTopBar(title, navigationIcon = "back", navigationDescription = "뒤로 가기", onNavigationClick = { backs.incrementAndGet() }, actions = {
                        MDiveIconButton("search", "검색 열기", { actions.incrementAndGet() })
                        MDiveIconButton("star", "즐겨찾기", {})
                    })
                }
            }
        }
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithContentDescription("검색 열기").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("즐겨찾기").assertIsDisplayed()
        compose.onNodeWithContentDescription("뒤로 가기").performClick()
        assertEquals(1, actions.get())
        assertEquals(1, backs.get())
    }
}
