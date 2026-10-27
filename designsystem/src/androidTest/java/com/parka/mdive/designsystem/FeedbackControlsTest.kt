package com.parka.mdive.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class FeedbackControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dialogConfirmationAndBackAreDistinctAndBusyDisablesActions() {
        var visible by mutableStateOf(true)
        var busy by mutableStateOf(false)
        var confirms = 0
        var dismissals = 0
        compose.setContent {
            MDiveDesignTheme {
                if (visible) MDiveConfirmationDialog("목록에서 제거할까요?", "원본 파일은 삭제하지 않습니다.", "목록에서 제거", { confirms++; visible = false }, { dismissals++; visible = false }, busy = busy)
            }
        }
        compose.onNodeWithText("목록에서 제거").performClick()
        compose.runOnIdle { assertEquals(1, confirms); assertEquals(0, dismissals); visible = true; busy = true }
        compose.onNodeWithText("목록에서 제거").assertIsNotEnabled()
        compose.onNodeWithText("취소").assertIsNotEnabled()
        Espresso.pressBack()
        compose.runOnIdle { assertTrue(visible); assertEquals(0, dismissals); busy = false }
        Espresso.pressBack()
        compose.runOnIdle { assertFalse(visible); assertEquals(1, dismissals); assertEquals(1, confirms) }
    }

    @Test
    fun longDialogAtLargeFontKeepsActionsVisible() {
        var confirmed = false
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                MDiveDesignTheme {
                    MDiveConfirmationDialog("긴 문서의 목록 제거 확인", "긴 설명입니다. 원본 파일은 삭제하지 않습니다. ".repeat(30), "제거 확인", { confirmed = true }, {})
                }
            }
        }
        compose.onNodeWithText("제거 확인").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(confirmed) }
    }

    @Test
    fun sheetCloseDismissesWithoutMutatingContent() {
        var visible by mutableStateOf(true)
        compose.setContent {
            MDiveDesignTheme {
                if (visible) MDiveBottomSheet("목차", { visible = false }) {
                    repeat(30) { Text("긴 항목 $it") }
                }
            }
        }
        compose.onNodeWithText("목차").assertIsDisplayed()
        compose.onNodeWithContentDescription("패널 닫기").assertIsDisplayed().performClick()
        compose.runOnIdle { assertFalse(visible) }
    }

    @Test
    fun sheetAnimatesEntryAndExitThenCanReopenWithoutDuplicateDismiss() {
        var visible by mutableStateOf(false)
        var dismissals = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MDiveDesignTheme {
                MDiveButton("패널 열기", { visible = true })
                if (visible) MDiveBottomSheet("움직이는 패널", { dismissals++; visible = false }) { requestDismiss ->
                    Text("본문은 패널 안에서 유지됩니다.")
                    MDiveButton("선택하고 닫기", { requestDismiss(); requestDismiss() })
                }
            }
        }
        compose.onNodeWithText("패널 열기").performClick()
        compose.mainClock.advanceTimeByFrame()
        val enteringTop = compose.onNodeWithContentDescription("패널 닫기").getUnclippedBoundsInRoot().top.value
        compose.mainClock.advanceTimeBy(1500)
        val settledTop = compose.onNodeWithContentDescription("패널 닫기").getUnclippedBoundsInRoot().top.value
        assertTrue("Sheet enters by moving upward", enteringTop > settledTop + 1f)
        compose.onNodeWithText("선택하고 닫기").performClick()
        compose.runOnIdle { assertTrue("Host is retained while hide is starting", visible); assertEquals(0, dismissals) }
        compose.mainClock.advanceTimeBy(64)
        val exitingTop = compose.onNodeWithContentDescription("패널 닫기").getUnclippedBoundsInRoot().top.value
        assertTrue("Sheet exits by moving downward", exitingTop > settledTop)
        compose.mainClock.advanceTimeBy(1500)
        compose.runOnIdle { assertFalse(visible); assertEquals("Repeated close requests notify once", 1, dismissals) }
        compose.onNodeWithText("패널 열기").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("움직이는 패널").assertExists()
        compose.mainClock.advanceTimeBy(1500)
        compose.onNodeWithText("움직이는 패널").assertIsDisplayed()
        compose.onNodeWithContentDescription("패널 닫기").performClick()
        compose.mainClock.advanceTimeBy(1500)
        compose.runOnIdle { assertFalse(visible); assertEquals(2, dismissals) }
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun draggingSheetDownDismissesAndReopeningCanDismissWithBack() {
        var visible by mutableStateOf(true)
        var dismissals = 0
        compose.setContent {
            MDiveDesignTheme {
                if (visible) MDiveBottomSheet("드래그 패널", { dismissals++; visible = false }) {
                    Text("짧은 내용")
                }
            }
        }
        val header = compose.onNodeWithContentDescription("패널 닫기").fetchSemanticsNode().boundsInRoot
        compose.onNode(isDialog()).performTouchInput {
            swipe(Offset(width / 2f, header.center.y), Offset(width / 2f, height - 2f), durationMillis = 160)
        }
        compose.runOnIdle { assertFalse("Downward swipe dismisses the modal sheet", visible); assertEquals(1, dismissals); visible = true }
        compose.onNodeWithText("드래그 패널").assertIsDisplayed()
        Espresso.pressBack()
        compose.runOnIdle { assertFalse(visible); assertEquals(2, dismissals) }
    }

    @Test
    fun sliderSupportsAccessibilityAndRightArrowInRtl() {
        var value by mutableFloatStateOf(5f)
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MDiveDesignTheme { MDiveSlider(value, { value = it }, 0f..10f, "크기", value.toString(), steps = 9) }
            }
        }
        val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(7f)) }
        compose.runOnIdle { assertEquals(7f, value) }
        slider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        slider.performKeyInput { keyDown(Key.DirectionRight); keyUp(Key.DirectionRight) }
        compose.runOnIdle { assertEquals(6f, value) }
    }

    @Test
    fun sliderTrackTapAndSlowDragChangeValueButDisabledGesturesDoNot() {
        var value by mutableFloatStateOf(5f)
        var enabled by mutableStateOf(true)
        compose.setContent {
            MDiveDesignTheme { MDiveSlider(value, { value = it }, 0f..10f, "크기", value.toString(), enabled = enabled, steps = 9) }
        }
        val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        slider.performTouchInput { click(Offset(width * .8f, height / 2f)) }
        compose.runOnIdle { assertTrue("Track tap moves the thumb toward the tapped value", value in 7f..9f) }
        val startFraction = value / 10f
        slider.performTouchInput {
            // Slow motion emits many deltas smaller than one discrete step.
            // The drag must accumulate rather than rounding every delta back to the old value.
            swipe(Offset(width * startFraction, height / 2f), Offset(width * .2f, height / 2f), durationMillis = 1500)
        }
        compose.runOnIdle { assertTrue("Slow dragging crosses discrete slider steps", value < 4f); enabled = false }
        val retained = value
        slider.assertIsNotEnabled().performTouchInput {
            click(Offset(width * .9f, height / 2f))
            swipe(Offset(width * .2f, height / 2f), Offset(width * .9f, height / 2f), durationMillis = 500)
        }
        compose.runOnIdle { assertEquals("Disabled pointer input must not change the value", retained, value) }
    }

    @Test
    fun tappingOutsideDismissesDialogAndSheetButDoesNotConfirmOrInterruptBusyDialog() {
        var visible by mutableStateOf(true)
        var sheet by mutableStateOf(false)
        var busy by mutableStateOf(false)
        var dismissals = 0
        var confirms = 0
        compose.setContent {
            MDiveDesignTheme {
                if (visible) {
                    if (sheet) MDiveBottomSheet("목차", { dismissals++; visible = false }) { Text("목차 내용") }
                    else MDiveConfirmationDialog("제거할까요?", "원본은 유지합니다.", "확인", { confirms++; visible = false }, { dismissals++; visible = false }, busy = busy)
                }
            }
        }
        fun tapOutside() = compose.onNode(isDialog()).performTouchInput { click(Offset(8f, height * .1f)) }
        tapOutside()
        compose.runOnIdle { assertFalse(visible); assertEquals(1, dismissals); assertEquals(0, confirms); sheet = true; visible = true }
        tapOutside()
        compose.runOnIdle { assertFalse(visible); assertEquals(2, dismissals); assertEquals(0, confirms); sheet = false; busy = true; visible = true }
        tapOutside()
        compose.runOnIdle { assertTrue(visible); assertEquals(2, dismissals); assertEquals(0, confirms); busy = false }
        tapOutside()
        compose.runOnIdle { assertFalse(visible); assertEquals(3, dismissals); assertEquals(0, confirms) }
    }

    @Test
    fun recoveryBannerPlacesRetryAtTrailingEdgeCenteredBesideMessage() {
        var retried = false
        compose.setContent {
            MDiveDesignTheme {
                MDiveRecoveryBanner("저장하지 못했어요", modifier = Modifier.width(360.dp).testTag("banner"), action = {
                    MDiveButton("다시 시도", { retried = true }, variant = MDiveButtonVariant.Quiet)
                })
            }
        }
        val message = compose.onNodeWithText("저장하지 못했어요").getUnclippedBoundsInRoot()
        val action = compose.onNodeWithText("다시 시도").getUnclippedBoundsInRoot()
        val banner = compose.onNodeWithTag("banner").getUnclippedBoundsInRoot()
        assertTrue("Action follows the message on the same row", action.left >= message.right)
        assertTrue("Message and action share their vertical center", kotlin.math.abs((action.top.value + action.bottom.value) / 2 - (message.top.value + message.bottom.value) / 2) < 1f)
        assertTrue(action.right <= banner.right)
        compose.onNodeWithText("다시 시도").performClick()
        compose.runOnIdle { assertTrue(retried) }
    }

    @Test
    fun recoveryBannerStacksTwoActionsInTheTrailingSlot() {
        compose.setContent {
            MDiveDesignTheme {
                MDiveRecoveryBanner("콘텐츠 상태", modifier = Modifier.width(360.dp), action = {
                    MDiveButton("원문 보기", {}, variant = MDiveButtonVariant.Outlined, size = MDiveButtonSize.S)
                    MDiveButton("콘텐츠 확대", {}, variant = MDiveButtonVariant.Outlined, size = MDiveButtonSize.S)
                })
            }
        }
        val first = compose.onNodeWithText("원문 보기").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText("콘텐츠 확대").getUnclippedBoundsInRoot()
        val message = compose.onNodeWithText("콘텐츠 상태").getUnclippedBoundsInRoot()
        assertTrue("Actions stack vertically", second.top >= first.bottom)
        assertTrue("Normal-width actions remain beside the message", first.left >= message.right && second.left >= message.right)
    }

    @Test
    fun recoveryBannerWrapsActionAtLargeFontWithoutOverlappingText() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                MDiveDesignTheme {
                    MDiveRecoveryBanner("책갈피 저장에 실패했어요", modifier = Modifier.width(260.dp).testTag("banner"), action = {
                        MDiveButton("다시 시도", {}, variant = MDiveButtonVariant.Quiet)
                    })
                }
            }
        }
        val message = compose.onNodeWithText("책갈피 저장에 실패했어요").getUnclippedBoundsInRoot()
        val action = compose.onNodeWithText("다시 시도").getUnclippedBoundsInRoot()
        val banner = compose.onNodeWithTag("banner").getUnclippedBoundsInRoot()
        assertTrue("Wrapped action starts below the readable message", action.top >= message.bottom)
        assertTrue("Wrapped action remains inside the banner", action.left >= banner.left && action.right <= banner.right && action.bottom <= banner.bottom)
        compose.onNodeWithText("다시 시도").assertIsDisplayed()
    }

    @Test
    fun recoveryBannerWithoutActionDoesNotReserveAnEmptyActionRow() {
        compose.setContent {
            MDiveDesignTheme { MDiveRecoveryBanner("저장 상태를 확인해 주세요", modifier = Modifier.width(360.dp).testTag("banner")) }
        }
        val message = compose.onNodeWithText("저장 상태를 확인해 주세요").getUnclippedBoundsInRoot()
        val banner = compose.onNodeWithTag("banner").getUnclippedBoundsInRoot()
        assertTrue("No empty action slot enlarges the message-only banner: $banner", (banner.bottom - banner.top).value <= 48f)
        compose.onNodeWithText("다시 시도").assertDoesNotExist()
    }

    @Test
    fun textFieldReportsValidationAndEditsControlledValue() {
        var value by mutableStateOf("원래 이름")
        compose.setContent {
            MDiveDesignTheme { MDiveTextField(value, { value = it }, "책갈피 이름", errorMessage = "이름을 확인해 주세요") }
        }
        compose.onNodeWithText("원래 이름").assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "이름을 확인해 주세요"))
            .performTextReplacement("새 이름")
        compose.runOnIdle { assertEquals("새 이름", value) }
    }
}
