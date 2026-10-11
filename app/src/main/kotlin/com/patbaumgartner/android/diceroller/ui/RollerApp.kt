package com.patbaumgartner.android.diceroller.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patbaumgartner.android.diceroller.feedback.DiceHaptics
import com.patbaumgartner.android.diceroller.feedback.DiceSound
import com.patbaumgartner.android.diceroller.sensor.ShakeDetector
import com.patbaumgartner.android.diceroller.ui.screens.HistoryScreen
import com.patbaumgartner.android.diceroller.ui.screens.PresetsScreen
import com.patbaumgartner.android.diceroller.ui.screens.RollerScreen
import com.patbaumgartner.android.diceroller.ui.screens.SettingsScreen

@Composable
fun RollerApp(viewModel: RollerViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val currentEntry by viewModel.currentEntry.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val lockedValues by viewModel.lockedValues.collectAsStateWithLifecycle()

    var screen by rememberSaveable { mutableStateOf(Screen.Roller) }

    // System back inside a sub-screen returns to the roller instead of leaving the app.
    BackHandler(enabled = screen != Screen.Roller) { screen = Screen.Roller }

    // Feedback lives for as long as the screen does; releasing the tone generator
    // on dispose keeps the audio resources bounded.
    val context = LocalContext.current
    val sound = remember { DiceSound() }
    val haptics = remember { DiceHaptics(context) }
    DisposableEffect(sound) { onDispose { sound.release() } }

    // Both the roll button and a shake go through the same feedback-and-roll path,
    // so the sound and haptics settings gate every roll identically.
    val roll: () -> Unit = {
        if (settings.soundEnabled) sound.play()
        if (settings.vibrationEnabled) haptics.tick()
        viewModel.roll()
    }

    if (settings.shakeToRollEnabled && screen == Screen.Roller) {
        ShakeEffect(onShake = roll)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Crossfade(targetState = screen, label = "screen") { current ->
            when (current) {
                Screen.Roller -> RollerScreen(
                    settings = settings,
                    currentEntry = currentEntry,
                    lockedValues = lockedValues,
                    onRoll = roll,
                    onToggleLock = viewModel::toggleLock,
                    onOpenPresets = { screen = Screen.Presets },
                    onOpenHistory = { screen = Screen.History },
                    onOpenSettings = { screen = Screen.Settings },
                )

                Screen.Presets -> PresetsScreen(
                    presets = presets,
                    onApply = { viewModel.applyPreset(it); screen = Screen.Roller },
                    onDelete = viewModel::deletePreset,
                    onBack = { screen = Screen.Roller },
                )

                Screen.History -> HistoryScreen(
                    history = history,
                    onClear = viewModel::clearHistory,
                    onBack = { screen = Screen.Roller },
                )

                Screen.Settings -> SettingsScreen(
                    settings = settings,
                    presets = presets,
                    onDisplayModeChange = viewModel::updateDisplayMode,
                    onAnimationSpeedChange = viewModel::updateAnimationSpeed,
                    onSoundChange = viewModel::updateSoundEnabled,
                    onVibrationChange = viewModel::updateVibrationEnabled,
                    onShakeChange = viewModel::updateShakeToRollEnabled,
                    onUpdateDiceCount = viewModel::updateDiceCount,
                    onUpdateDieConfig = viewModel::updateDieConfig,
                    onSavePreset = viewModel::savePreset,
                    onApplyPreset = viewModel::applyPreset,
                    onDeletePreset = viewModel::deletePreset,
                    onBack = { screen = Screen.Roller },
                )
            }
        }
    }
}

enum class Screen { Roller, Presets, History, Settings }

@Composable
private fun ShakeEffect(onShake: () -> Unit) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)
    DisposableEffect(Unit) {
        val detector = ShakeDetector(context) { currentOnShake() }
        detector.start()
        onDispose { detector.stop() }
    }
}
