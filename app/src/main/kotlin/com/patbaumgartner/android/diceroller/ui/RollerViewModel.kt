package com.patbaumgartner.android.diceroller.ui

import com.patbaumgartner.android.diceroller.data.settings.SettingsRepository
import androidx.lifecycle.ViewModel
import com.patbaumgartner.android.diceroller.domain.model.AnimationSpeed
import com.patbaumgartner.android.diceroller.domain.model.DicePreset
import com.patbaumgartner.android.diceroller.domain.model.DieConfig
import com.patbaumgartner.android.diceroller.domain.model.DisplayMode
import com.patbaumgartner.android.diceroller.domain.model.RollEntry
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import com.patbaumgartner.android.diceroller.domain.usecase.RollDiceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RollerViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val rollDice = RollDiceUseCase()

    private val _settings = MutableStateFlow(settingsRepository.loadSettings())
    val settings: StateFlow<RollSettings> = _settings.asStateFlow()

    private val _presets = MutableStateFlow(settingsRepository.loadPresets())
    val presets: StateFlow<List<DicePreset>> = _presets.asStateFlow()

    private val _currentEntry = MutableStateFlow<RollEntry?>(null)
    val currentEntry: StateFlow<RollEntry?> = _currentEntry.asStateFlow()

    private val _history = MutableStateFlow<List<RollEntry>>(emptyList())
    val history: StateFlow<List<RollEntry>> = _history.asStateFlow()

    private val _lockedValues = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val lockedValues: StateFlow<Map<Int, Int>> = _lockedValues.asStateFlow()

    private val _isRolling = MutableStateFlow(false)
    val isRolling: StateFlow<Boolean> = _isRolling.asStateFlow()

    fun roll() {
        val entry = rollDice.roll(_settings.value, _lockedValues.value)

        _currentEntry.value = entry
        _history.update { history ->
            (listOf(entry) + history).take(HISTORY_LIMIT)
        }
    }

    fun toggleLock(index: Int, value: Int) {
        _lockedValues.update { locked ->
            if (locked.containsKey(index)) {
                locked - index
            } else {
                locked + (index to value)
            }
        }
    }

    fun clearLocked() {
        _lockedValues.value = emptyMap()
    }

    fun updateSettings(newSettings: RollSettings) {
        _settings.value = newSettings
        settingsRepository.saveSettings(newSettings)
    }

    fun updateDiceCount(count: Int) {
        updateSettings(_settings.value.copy(diceCount = count.coerceIn(MIN_DICE, MAX_DICE)))
    }

    fun updateDieConfig(config: DieConfig) {
        updateSettings(_settings.value.copy(dieConfig = config))
    }

    fun updateDisplayMode(mode: DisplayMode) {
        updateSettings(_settings.value.copy(displayMode = mode))
    }

    fun updateAnimationSpeed(speed: AnimationSpeed) {
        updateSettings(_settings.value.copy(animationSpeed = speed))
    }

    fun updateSoundEnabled(enabled: Boolean) {
        updateSettings(_settings.value.copy(soundEnabled = enabled))
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        updateSettings(_settings.value.copy(vibrationEnabled = enabled))
    }

    fun updateShakeToRollEnabled(enabled: Boolean) {
        updateSettings(_settings.value.copy(shakeToRollEnabled = enabled))
    }

    fun savePreset(name: String): Boolean {
        if (_presets.value.size >= MAX_PRESETS) return false
        val preset = DicePreset(name = name, settings = _settings.value)
        settingsRepository.savePreset(preset)
        _presets.value = settingsRepository.loadPresets()
        return true
    }

    fun applyPreset(preset: DicePreset) {
        updateSettings(preset.settings)
    }

    fun deletePreset(presetId: String) {
        settingsRepository.deletePreset(presetId)
        _presets.value = settingsRepository.loadPresets()
    }

    fun resetRoll() {
        _currentEntry.value = null
        clearLocked()
    }

    companion object {
        const val MIN_DICE = 1
        const val MAX_DICE = 40
        const val MAX_PRESETS = 10
        private const val HISTORY_LIMIT = 50
    }
}
