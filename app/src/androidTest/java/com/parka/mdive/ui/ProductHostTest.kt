package com.parka.mdive.ui

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.lifecycle.Lifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.espresso.Espresso
import com.parka.mdive.designsystem.MDiveDesignTheme
import com.parka.mdive.testing.ProductTestActivity
import com.parka.mdive.ui.base.CollectEffects
import com.parka.mdive.ui.navigation.Destination
import com.parka.mdive.ui.navigation.MDiveApp
import com.parka.mdive.ui.reader.ReaderContent
import com.parka.mdive.ui.reader.ReaderEffect
import com.parka.mdive.ui.reader.ReaderScreen
import com.parka.mdive.ui.reader.ReaderState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ProductHostTest {
    @get:Rule val compose = createAndroidComposeRule<ProductTestActivity>()

    @Test
    fun hiltHomeStartsWithoutPreparedDocuments() {
        compose.setContent { MDiveDesignTheme { MDiveApp() } }
        compose.onNodeWithText("반가워요.").assertIsDisplayed()
        compose.onNodeWithText("파일 열기").assertIsNotEnabled()
        compose.onNodeWithContentDescription("MDive").assertIsDisplayed()
        compose.onNodeWithText("MDive").assertDoesNotExist()
        val image = compose.onNodeWithTag("welcome-illustration").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("반가워요.").fetchSemanticsNode().boundsInRoot
        val detail = compose.onNodeWithText("Markdown 문서를 열고 편안하게 읽어보세요.\n아직 파일 열기는 사용할 수 없어요.").fetchSemanticsNode().boundsInRoot
        assertEquals(28 * compose.density.density, title.top - image.bottom, 1f)
        assertEquals(8 * compose.density.density, detail.top - title.bottom, 1f)
    }

    @Test
    fun readerLoadingEmptyAndFailureRenderAndForwardRecoveryActions() {
        val state = androidx.compose.runtime.mutableStateOf(ReaderState("테스트.md"))
        var back = 0
        var retry = 0
        compose.setContent {
            MDiveDesignTheme {
                ReaderScreen(state.value, { back++ }, { retry++ })
            }
        }
        compose.onNodeWithContentDescription("문서를 준비하고 있어요").assertIsDisplayed()
        compose.runOnUiThread { state.value = state.value.copy(content = ReaderContent.Empty) }
        compose.onNodeWithText("빈 문서예요").assertIsDisplayed()
        compose.runOnUiThread { state.value = state.value.copy(content = ReaderContent.Failure) }
        compose.onNodeWithText("문서를 열지 못했어요").assertIsDisplayed()
        compose.onNodeWithText("다시 시도").performClick()
        compose.onNodeWithText("다시 선택").assertDoesNotExist()
        compose.onNodeWithText("홈으로").performClick()
        compose.runOnIdle {
            assertEquals(1, retry)
            assertEquals(1, back)
        }
    }

    @Test
    fun navigationTopBackAndSystemBackReturnToHome() {
        val stack = NavBackStack<NavKey>(Destination.Home, Destination.Reader)
        compose.setContent { MDiveDesignTheme { MDiveApp(backStack = stack) } }
        compose.onNodeWithContentDescription("문서를 준비하고 있어요").assertIsDisplayed()
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
        compose.onNodeWithText("반가워요.").assertIsDisplayed()
        compose.runOnIdle { stack.add(Destination.Reader) }
        compose.onNodeWithContentDescription("문서를 준비하고 있어요").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithText("반가워요.").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf(Destination.Home), stack.toList())
            assertFalse(compose.activity.isFinishing)
        }
    }

    @Test
    fun navigationRestoresSerializableKeysAndStillReturnsHome() {
        val restoration = StateRestorationTester(compose)
        var currentStack: NavBackStack<NavKey>? = null
        restoration.setContent {
            val stack = rememberNavBackStack(Destination.Home)
            currentStack = stack
            MDiveDesignTheme { MDiveApp(backStack = stack) }
        }
        compose.runOnIdle { currentStack!!.add(Destination.Reader) }
        compose.onNodeWithContentDescription("문서를 준비하고 있어요").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("문서를 준비하고 있어요").assertIsDisplayed()
        compose.onNodeWithContentDescription("홈으로 돌아가기").performClick()
        compose.onNodeWithText("반가워요.").assertIsDisplayed()
    }

    @Test
    fun effectsWaitWhileStoppedAndAreConsumedOnceOnRestart() {
        val channel = Channel<ReaderEffect>(Channel.BUFFERED)
        var received = 0
        compose.setContent {
            val effects = remember { channel.receiveAsFlow() }
            CollectEffects(effects) { received++ }
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        channel.trySend(ReaderEffect.NavigateHome).getOrThrow()
        assertEquals(0, received)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.runOnIdle { assertEquals(1, received) }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.runOnIdle { assertEquals(1, received) }
    }
}
