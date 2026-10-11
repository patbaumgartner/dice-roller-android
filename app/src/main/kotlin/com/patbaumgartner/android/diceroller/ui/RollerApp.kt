package com.patbaumgartner.android.diceroller.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patbaumgartner.android.diceroller.R
import com.patbaumgartner.android.diceroller.sensor.ShakeDetector
import com.patbaumgartner.android.diceroller.ui.screens.PresetsScreen
import com.patbaumgartner.android.diceroller.ui.screens.RollerScreen
import com.patbaumgartner.android.diceroller.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RollerApp(viewModel: RollerViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val currentEntry by viewModel.currentEntry.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val lockedValues by viewModel.lockedValues.collectAsStateWithLifecycle()

    var screen by rememberSaveable { mutableStateOf(Screen.Roller) }

    if (settings.shakeToRollEnabled && screen == Screen.Roller) {
        ShakeEffect(onShake = viewModel::roll)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (screen) {
                            Screen.Roller -> stringResource(R.string.screen_roller)
                            Screen.Presets -> stringResource(R.string.screen_presets)
                            Screen.Settings -> stringResource(R.string.screen_settings)
                        },
                    )
                },
                actions = {
                    if (screen == Screen.Roller) {
                        IconButton(onClick = { screen = Screen.Presets }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.cd_presets))
                        }
                        IconButton(onClick = { screen = Screen.Settings }) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.cd_settings))
                        }
                    } else {
                        IconButton(onClick = { screen = Screen.Roller }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (screen) {
            Screen.Roller -> RollerScreen(
                settings = settings,
                currentEntry = currentEntry,
                lockedValues = lockedValues,
                history = history,
                onRoll = viewModel::roll,
                onToggleLock = viewModel::toggleLock,
                onClearLocked = viewModel::clearLocked,
                onReset = viewModel::resetRoll,
                onUpdateDiceCount = viewModel::updateDiceCount,
                onUpdateDieConfig = viewModel::updateDieConfig,
                modifier = Modifier.padding(padding),
            )

            Screen.Presets -> PresetsScreen(
                presets = presets,
                onApply = { viewModel.applyPreset(it); screen = Screen.Roller },
                onDelete = { viewModel.deletePreset(it.id) },
                modifier = Modifier.padding(padding),
            )

            Screen.Settings -> SettingsScreen(
                settings = settings,
                presets = presets,
                onDisplayModeChange = viewModel::updateDisplayMode,
                onAnimationSpeedChange = viewModel::updateAnimationSpeed,
                onSoundChange = viewModel::updateSoundEnabled,
                onVibrationChange = viewModel::updateVibrationEnabled,
                onShakeChange = viewModel::updateShakeToRollEnabled,
                onSavePreset = viewModel::savePreset,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

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

enum class Screen { Roller, Presets, Settings }
