package com.patbaumgartner.android.diceroller.ui

import androidx.lifecycle.ViewModel
import com.patbaumgartner.android.diceroller.data.settings.PresetStorageException
import com.patbaumgartner.android.diceroller.data.settings.SettingsStore
import com.patbaumgartner.android.diceroller.domain.model.AnimationSpeed
import com.patbaumgartner.android.diceroller.domain.model.DicePreset
import com.patbaumgartner.android.diceroller.domain.model.DieConfig
import com.patbaumgartner.android.diceroller.domain.model.DisplayMode
import com.patbaumgartner.android.diceroller.domain.model.Limits
import com.patbaumgartner.android.diceroller.domain.model.RollEntry
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import com.patbaumgartner.android.diceroller.domain.usecase.RollDiceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RollerViewModel(
    private val settingsRepository: SettingsStore,
) : ViewModel() {

    private val rollDice = RollDiceUseCase()

    private val _settings = MutableStateFlow(settingsRepository.loadSettings())
    val settings: StateFlow<RollSettings> = _settings.asStateFlow()

    private val _presets = MutableStateFlow(emptyList<DicePreset>())
    val presets: StateFlow<List<DicePreset>> = _presets.asStateFlow()

    private val _presetStorageError = MutableStateFlow(false)
    val presetStorageError: StateFlow<Boolean> = _presetStorageError.asStateFlow()

    private val _currentEntry = MutableStateFlow<RollEntry?>(null)
    val currentEntry: StateFlow<RollEntry?> = _currentEntry.asStateFlow()

    private val _history = MutableStateFlow<List<RollEntry>>(emptyList())
    val history: StateFlow<List<RollEntry>> = _history.asStateFlow()

    private val _lockedValues = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val lockedValues: StateFlow<Map<Int, Int>> = _lockedValues.asStateFlow()

    private val _isRolling = MutableStateFlow(false)
    val isRolling: StateFlow<Boolean> = _isRolling.asStateFlow()

    init {
        refreshPresets()
    }

    fun roll() {
        val entry = rollDice.roll(_settings.value, _lockedValues.value)
        _currentEntry.value = entry
        _history.update { history -> Limits.cappedHistory(listOf(entry) + history) }
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
        val previousSettings = _settings.value
        val dieConfigChanged = previousSettings.dieConfig != newSettings.dieConfig
        val diceCountChanged = previousSettings.diceCount != newSettings.diceCount

        if (dieConfigChanged) {
            clearLocked()
        } else if (newSettings.diceCount < previousSettings.diceCount) {
            _lockedValues.update { locked ->
                locked.filterKeys { index -> index < newSettings.diceCount }
            }
        }
        if (dieConfigChanged || diceCountChanged) {
            _currentEntry.value = null
        }
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

    fun savePreset(name: String): PresetSaveResult {
        if (!Limits.canAddPreset(_presets.value.size)) return PresetSaveResult.LIMIT_REACHED
        val preset = DicePreset(name = name, settings = _settings.value)
        return try {
            if (!settingsRepository.savePreset(preset)) return PresetSaveResult.LIMIT_REACHED
            refreshPresets()
            if (_presetStorageError.value) PresetSaveResult.STORAGE_ERROR else PresetSaveResult.SAVED
        } catch (_: PresetStorageException) {
            _presetStorageError.value = true
            PresetSaveResult.STORAGE_ERROR
        }
    }

    fun applyPreset(preset: DicePreset) {
        updateSettings(preset.settings)
    }

    fun deletePreset(presetId: String) {
        try {
            settingsRepository.deletePreset(presetId)
            refreshPresets()
        } catch (_: PresetStorageException) {
            _presetStorageError.value = true
        }
    }

    /** History is per session, so clearing it needs no repository call. */
    fun clearHistory() {
        _history.value = emptyList()
    }

    fun resetRoll() {
        _currentEntry.value = null
        clearLocked()
    }

    private fun refreshPresets() {
        try {
            _presets.value = settingsRepository.loadPresets()
            _presetStorageError.value = false
        } catch (_: PresetStorageException) {
            _presetStorageError.value = true
        }
    }

    companion object {
        const val MIN_DICE = Limits.MIN_DICE
        const val MAX_DICE = Limits.MAX_DICE
        const val MAX_PRESETS = Limits.MAX_PRESETS
    }

    enum class PresetSaveResult {
        SAVED,
        LIMIT_REACHED,
        STORAGE_ERROR,
    }
}
