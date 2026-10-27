package com.parka.mdive.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.parka.mdive.designsystem.CatalogContent
import org.junit.Rule
import org.junit.Test

class CatalogFeedbackReviewTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun screenSkeletonExamplesHaveDistinctAccessibleDescriptionsAndRecoveryRemains() {
        compose.setContent {
            CatalogContent(darkTheme = false, onToggleTheme = {}, selectedEntryId = "feedback")
        }
        compose.onNodeWithContentDescription("읽기 본문 로딩 예시").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("홈 목록 로딩 예시").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("첫 문서를 열어보세요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("파일 열기 · 시연").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("홈으로 · 시연").performScrollTo().assertIsDisplayed()
    }
}
