package com.patbaumgartner.android.diceroller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patbaumgartner.android.diceroller.R
import com.patbaumgartner.android.diceroller.domain.model.AnimationSpeed
import com.patbaumgartner.android.diceroller.domain.model.DicePreset
import com.patbaumgartner.android.diceroller.domain.model.DisplayMode
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import com.patbaumgartner.android.diceroller.ui.RollerViewModel

@Composable
fun SettingsScreen(
    settings: RollSettings,
    presets: List<DicePreset>,
    onDisplayModeChange: (DisplayMode) -> Unit,
    onAnimationSpeedChange: (AnimationSpeed) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onShakeChange: (Boolean) -> Unit,
    onSavePreset: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var presetName by remember { mutableStateOf("") }
    var saveFeedback by remember { mutableStateOf<String?>(null) }
    val presetSavedMessage = stringResource(R.string.preset_saved)
    val maxPresetsMessage = stringResource(R.string.max_presets_reached)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsSection(stringResource(R.string.settings_display_section)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DisplayMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.displayMode == mode,
                        onClick = { onDisplayModeChange(mode) },
                        label = {
                            Text(
                                if (mode == DisplayMode.DOTS) {
                                    stringResource(R.string.display_dots)
                                } else {
                                    stringResource(R.string.display_numbers)
                                },
                            )
                        },
                    )
                }
            }
        }
        SettingsSection(stringResource(R.string.settings_animation_section)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimationSpeed.entries.forEach { speed ->
                    FilterChip(
                        selected = settings.animationSpeed == speed,
                        onClick = { onAnimationSpeedChange(speed) },
                        label = { Text(speed.label()) },
                    )
                }
            }
        }
        ToggleRow(stringResource(R.string.settings_sound), settings.soundEnabled, onSoundChange)
        ToggleRow(stringResource(R.string.settings_haptics), settings.vibrationEnabled, onVibrationChange)
        ToggleRow(stringResource(R.string.settings_shake), settings.shakeToRollEnabled, onShakeChange)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text(stringResource(R.string.preset_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = {
                    if (presetName.isNotBlank()) {
                        val saved = onSavePreset(presetName)
                        saveFeedback =
                            if (saved) presetSavedMessage else maxPresetsMessage
                        if (saved) presetName = ""
                    }
                },
                enabled = presets.size < RollerViewModel.MAX_PRESETS && presetName.isNotBlank(),
            ) {
                Text(stringResource(R.string.settings_save_preset))
            }
            saveFeedback?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun AnimationSpeed.label(): String = when (this) {
    AnimationSpeed.OFF -> stringResource(R.string.animation_off)
    AnimationSpeed.SLOW -> stringResource(R.string.animation_slow)
    AnimationSpeed.NORMAL -> stringResource(R.string.animation_normal)
    AnimationSpeed.FAST -> stringResource(R.string.animation_fast)
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
