package com.patbaumgartner.android.diceroller.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.patbaumgartner.android.diceroller.R
import com.patbaumgartner.android.diceroller.domain.model.AnimationSpeed
import com.patbaumgartner.android.diceroller.domain.model.RollEntry
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import com.patbaumgartner.android.diceroller.ui.screens.RollerScreen
import com.patbaumgartner.android.diceroller.ui.theme.DiceRed
import com.patbaumgartner.android.diceroller.ui.theme.DiceRollerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiceAppearanceTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun previewDiceHaveTheSameFinishAsRolledDice() {
        val settings = RollSettings(diceCount = 1, animationSpeed = AnimationSpeed.OFF)
        val entry = mutableStateOf<RollEntry?>(null)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val unlocked = context.getString(R.string.cd_die_unlocked)

        compose.setContent {
            DiceRollerTheme {
                RollerScreen(
                    settings = settings,
                    currentEntry = entry.value,
                    lockedValues = emptyMap(),
                    onRoll = { entry.value = RollEntry(values = listOf(1), config = settings.dieConfig) },
                    onToggleLock = { _, _ -> },
                    onOpenPresets = {},
                    onOpenHistory = {},
                    onOpenSettings = {},
                )
            }
        }

        val preview = compose.onNode(hasStateDescription(unlocked)).captureToImage().toPixelMap()
        val upperFace = preview[preview.width / 2, preview.height / 4]
        assertTrue("die face must not have a milky white overlay", upperFace.green <= DiceRed.green)
        assertTrue("die face must keep its saturated accent", upperFace.blue <= DiceRed.blue)
        compose.runOnIdle {
            entry.value = RollEntry(values = listOf(1), config = settings.dieConfig)
        }
        val rolled = compose.onNode(hasStateDescription(unlocked)).captureToImage().toPixelMap()

        assertEquals("dice widths must match", preview.width, rolled.width)
        assertEquals("dice heights must match", preview.height, rolled.height)
        for (y in 0 until preview.height) {
            for (x in 0 until preview.width) {
                assertEquals("preview and rolled die pixel at ($x, $y)", rolled[x, y], preview[x, y])
            }
        }
    }
}
