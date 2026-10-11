package com.patbaumgartner.android.diceroller.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.patbaumgartner.android.diceroller.R
import com.patbaumgartner.android.diceroller.data.settings.PresetStorageException
import com.patbaumgartner.android.diceroller.data.settings.SettingsRepository
import com.patbaumgartner.android.diceroller.domain.model.DicePreset
import com.patbaumgartner.android.diceroller.domain.model.DisplayMode
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import com.patbaumgartner.android.diceroller.domain.model.DieType
import com.patbaumgartner.android.diceroller.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
        compose.onNodeWithContentDescription(str(R.string.action_roll))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertWidthIsEqualTo(120.dp)
            .assertHeightIsEqualTo(120.dp)
    }

    @Test
    fun tappingRollShowsATotal() {
        compose.onNodeWithContentDescription(str(R.string.action_roll)).performClick()
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
        compose.onNodeWithContentDescription(str(R.string.action_roll)).assertIsDisplayed()
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
        compose.onNodeWithContentDescription(str(R.string.action_roll)).performClick()
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

    @Test
    fun fortyDiceKeepTheRollControlReachable() {
        compose.onNodeWithContentDescription(str(R.string.cd_menu)).performClick()
        compose.onNodeWithText(str(R.string.screen_settings)).performClick()
        compose.onNodeWithTag("dice-count-slider").performSemanticsAction(SemanticsActions.SetProgress) {
            setProgress -> setProgress(40f)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.dice_count_label, 40)).assertIsDisplayed()

        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        compose.onNodeWithContentDescription(str(R.string.action_roll)).assertIsDisplayed()
        compose.onAllNodes(hasStateDescription(str(R.string.cd_die_unlocked))).assertCountEquals(40)
        compose.onNodeWithContentDescription(str(R.string.action_roll)).performClick()
        compose.onAllNodes(hasStateDescription(str(R.string.cd_die_unlocked))).assertCountEquals(40)
    }

    @Test
    fun diceExposeTheirLockStateToAccessibility() {
        compose.onNodeWithContentDescription(str(R.string.action_roll)).performClick()
        val unlockedCount = compose
            .onAllNodes(hasStateDescription(str(R.string.cd_die_unlocked)))
            .fetchSemanticsNodes()
            .size
        assertTrue("rolling must render at least one die", unlockedCount > 0)

        compose.onAllNodes(hasStateDescription(str(R.string.cd_die_unlocked)))[0].performClick()

        compose.onAllNodes(hasStateDescription(str(R.string.cd_die_locked))).assertCountEquals(1)
        compose.onAllNodes(hasStateDescription(str(R.string.cd_die_unlocked))).assertCountEquals(unlockedCount - 1)
    }

    @Test
    fun malformedPresetJsonIsPreservedWhenLoadingOrMutating() {
        val preferences = compose.activity.getSharedPreferences("dice_roller_prefs", Context.MODE_PRIVATE)
        val previousValue = preferences.getString("dice_presets", null)
        val malformedValue = "{malformed"
        val repository = SettingsRepository(compose.activity)

        try {
            check(preferences.edit().putString("dice_presets", malformedValue).commit())

            assertThrows(PresetStorageException::class.java) { repository.loadPresets() }
            assertThrows(PresetStorageException::class.java) {
                repository.savePreset(DicePreset(name = "New preset", settings = RollSettings()))
            }
            assertThrows(PresetStorageException::class.java) { repository.deletePreset("existing") }
            assertEquals(
                "failed operations must preserve the original value",
                malformedValue,
                preferences.getString("dice_presets", null),
            )
        } finally {
            if (previousValue == null) {
                check(preferences.edit().remove("dice_presets").commit())
            } else {
                check(preferences.edit().putString("dice_presets", previousValue).commit())
            }
        }
    }
}
