package com.patbaumgartner.android.diceroller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patbaumgartner.android.diceroller.R
import com.patbaumgartner.android.diceroller.domain.model.AnimationSpeed
import com.patbaumgartner.android.diceroller.domain.model.DicePreset
import com.patbaumgartner.android.diceroller.domain.model.DieConfig
import com.patbaumgartner.android.diceroller.domain.model.DieType
import com.patbaumgartner.android.diceroller.domain.model.DisplayMode
import com.patbaumgartner.android.diceroller.domain.model.Limits
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import com.patbaumgartner.android.diceroller.ui.DiceRollerIcons
import com.patbaumgartner.android.diceroller.ui.RollerViewModel

/**
 * Everything that shapes a roll lives here, in one calm scrollable page with
 * a back arrow — the reference app's settings pattern.
 */
@Composable
internal fun SettingsScreen(
    settings: RollSettings,
    presets: List<DicePreset>,
    onDisplayModeChange: (DisplayMode) -> Unit,
    onAnimationSpeedChange: (AnimationSpeed) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onShakeChange: (Boolean) -> Unit,
    onUpdateDiceCount: (Int) -> Unit,
    onUpdateDieConfig: (DieConfig) -> Unit,
    onSavePreset: (String) -> Boolean,
    onApplyPreset: (DicePreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onBack: () -> Unit,
) {
    var presetName by remember { mutableStateOf("") }
    var saveFeedback by remember { mutableStateOf<String?>(null) }
    var licensesShown by remember { mutableStateOf(false) }
    val presetSavedMessage = stringResource(R.string.preset_saved)
    val maxPresetsMessage = stringResource(R.string.max_presets_reached)
    val resources = LocalResources.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
    ) {
        PageHeader(stringResource(R.string.screen_settings), onBack)

        Column(
            modifier = Modifier.padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            SettingsSection(stringResource(R.string.settings_dice_section)) {
                DiceCountRow(settings.diceCount, onUpdateDiceCount)
                DieTypeRow(settings.dieConfig, onUpdateDieConfig)
            }
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
                            shape = RoundedCornerShape(12.dp),
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
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
            }
            ToggleRow(stringResource(R.string.settings_sound), settings.soundEnabled, onSoundChange)
            ToggleRow(stringResource(R.string.settings_haptics), settings.vibrationEnabled, onVibrationChange)
            ToggleRow(stringResource(R.string.settings_shake), settings.shakeToRollEnabled, onShakeChange)

            SavePresetSection(
                presetName = presetName,
                onPresetNameChange = { presetName = it },
                canSave = presets.size < RollerViewModel.MAX_PRESETS,
                onSave = {
                    val saved = onSavePreset(presetName)
                    saveFeedback = if (saved) presetSavedMessage else maxPresetsMessage
                    if (saved) presetName = ""
                },
                feedback = saveFeedback,
            )
            TextButton(
                onClick = { licensesShown = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(R.string.settings_open_source_licenses))
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (licensesShown) {
        val licenseText = remember(resources) {
            resources.openRawResource(R.raw.manrope_license)
                .bufferedReader()
                .use { it.readText() }
        }
        AlertDialog(
            onDismissRequest = { licensesShown = false },
            title = { Text(stringResource(R.string.settings_open_source_licenses_title)) },
            text = {
                Text(
                    text = licenseText,
                    modifier = Modifier.height(360.dp).verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = { licensesShown = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }
}

@Composable
private fun DiceCountRow(count: Int, onCountChange: (Int) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.dice_count_label, count),
            style = MaterialTheme.typography.titleMedium,
        )
        Slider(
            value = count.toFloat(),
            onValueChange = { onCountChange(it.toInt()) },
            valueRange = RollerViewModel.MIN_DICE.toFloat()..RollerViewModel.MAX_DICE.toFloat(),
            steps = RollerViewModel.MAX_DICE - RollerViewModel.MIN_DICE - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun DieTypeRow(config: DieConfig, onConfigChange: (DieConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.die_type_label),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DieType.entries.forEach { type ->
                FilterChip(
                    selected = config.type == type,
                    onClick = {
                        onConfigChange(
                            if (type == DieType.CUSTOM) {
                                // Keep the current bounds when switching to a custom range.
                                DieConfig.custom(config.minValue, config.maxValue)
                            } else {
                                DieConfig(type = type, minValue = 1, maxValue = type.sides, isCustom = false)
                            },
                        )
                    },
                    label = { Text(type.label, fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }
        if (config.isCustom) {
            Text(
                text = stringResource(R.string.custom_range_label, config.minValue, config.maxValue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RangeSlider(
                value = config.minValue.toFloat()..config.maxValue.toFloat(),
                onValueChange = { range ->
                    onConfigChange(DieConfig.custom(range.start.toInt(), range.endInclusive.toInt()))
                },
                valueRange = Limits.MIN_CUSTOM.toFloat()..Limits.MAX_CUSTOM.toFloat(),
                steps = Limits.MAX_CUSTOM - Limits.MIN_CUSTOM - 1,
            )
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

@Composable
private fun SavePresetSection(
    presetName: String,
    onPresetNameChange: (String) -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
    feedback: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = presetName,
            onValueChange = onPresetNameChange,
            label = { Text(stringResource(R.string.preset_name_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        Button(
            onClick = onSave,
            enabled = canSave && presetName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.settings_save_preset))
        }
        feedback?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun PageHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                DiceRollerIcons.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
            )
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}
