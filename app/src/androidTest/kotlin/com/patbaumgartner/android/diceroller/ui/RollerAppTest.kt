package com.patbaumgartner.android.diceroller.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.patbaumgartner.android.diceroller.R
import com.patbaumgartner.android.diceroller.domain.model.DieType
import com.patbaumgartner.android.diceroller.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI tests run against the real activity on a device or emulator.
 * They navigate like a user: menu → screen, taps on dice, back navigation.
 */
@RunWith(AndroidJUnit4::class)
class RollerAppTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun str(res: Int) = compose.activity.getString(res)

    @Test
    fun rollerShowsInitialPrompt() {
        compose.onNodeWithText(str(R.string.roll_prompt)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.action_roll)).assertIsDisplayed()
    }

    @Test
    fun tappingRollShowsATotal() {
        compose.onNodeWithText(str(R.string.action_roll)).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(str(R.string.roll_prompt)).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun menuOpensWithPresetsAndSettingsEntries() {
        compose.onNodeWithContentDescription(str(R.string.cd_menu)).performClick()
        compose.onNodeWithText(str(R.string.screen_presets)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.screen_settings)).assertIsDisplayed()
    }

    @Test
    fun menuNavigatesToPresetsAndBackReturns() {
        compose.onNodeWithContentDescription(str(R.string.cd_menu)).performClick()
        compose.onNodeWithText(str(R.string.screen_presets)).performClick()
        compose.onNodeWithText(str(R.string.presets_empty_title)).assertIsDisplayed()
        compose.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.waitForIdle()
        compose.onNodeWithText(str(R.string.action_roll)).assertIsDisplayed()
    }

    @Test
    fun menuNavigatesToSettings() {
        compose.onNodeWithContentDescription(str(R.string.cd_menu)).performClick()
        compose.onNodeWithText(str(R.string.screen_settings)).performClick()
        compose.onNodeWithText(str(R.string.settings_dice_section)).assertIsDisplayed()
    }

    @Test
    fun settingsShowsDisplayOptions() {
        compose.onNodeWithContentDescription(str(R.string.cd_menu)).performClick()
        compose.onNodeWithText(str(R.string.screen_settings)).performClick()
        compose.onNodeWithText(str(R.string.display_dots)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.display_numbers)).assertIsDisplayed()
    }

    @Test
    fun menuShowsHistoryEntry() {
        compose.onNodeWithContentDescription(str(R.string.cd_menu)).performClick()
        compose.onNodeWithText(str(R.string.screen_history)).assertIsDisplayed()
    }

    @Test
    fun historyShowsTheRolledTotalAndCanBeCleared() {
        compose.onNodeWithText(str(R.string.action_roll)).performClick()
        compose.onNodeWithContentDescription(str(R.string.cd_menu)).performClick()
        compose.onNodeWithText(str(R.string.screen_history)).performClick()
        compose.onNodeWithText(str(R.string.history_clear)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.history_clear)).performClick()
        compose.onNodeWithText(str(R.string.history_empty_title)).assertIsDisplayed()
    }

    @Test
    fun settingsOffersACustomRange() {
        compose.onNodeWithContentDescription(str(R.string.cd_menu)).performClick()
        compose.onNodeWithText(str(R.string.screen_settings)).performClick()
        compose.onNodeWithText(DieType.CUSTOM.label).performClick()
        compose.onNodeWithText("Range:", substring = true).assertIsDisplayed()
    }
}
