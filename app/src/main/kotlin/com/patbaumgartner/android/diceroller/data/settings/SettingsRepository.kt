package com.patbaumgartner.android.diceroller.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.patbaumgartner.android.diceroller.domain.model.AnimationSpeed
import com.patbaumgartner.android.diceroller.domain.model.DicePreset
import com.patbaumgartner.android.diceroller.domain.model.DieConfig
import com.patbaumgartner.android.diceroller.domain.model.DieType
import com.patbaumgartner.android.diceroller.domain.model.DisplayMode
import com.patbaumgartner.android.diceroller.domain.model.Limits
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import com.patbaumgartner.android.diceroller.domain.model.normalized
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface SettingsStore {
    fun loadSettings(): RollSettings

    fun saveSettings(settings: RollSettings)

    fun loadPresets(): List<DicePreset>

    fun savePreset(preset: DicePreset): Boolean

    fun deletePreset(presetId: String)
}

class SettingsRepository(context: Context) : SettingsStore {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun loadSettings(): RollSettings {
        val dieConfig = loadDieConfig()
        return RollSettings(
            diceCount = prefs.getInt(KEY_DICE_COUNT, 2),
            dieConfig = dieConfig,
            displayMode = DisplayMode.entries.getOrNull(prefs.getInt(KEY_DISPLAY_MODE, 0)) ?: DisplayMode.DOTS,
            animationSpeed = AnimationSpeed.entries.getOrNull(prefs.getInt(KEY_ANIMATION_SPEED, 2)) ?: AnimationSpeed.NORMAL,
            soundEnabled = prefs.getBoolean(KEY_SOUND, true),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
            shakeToRollEnabled = prefs.getBoolean(KEY_SHAKE, false),
        ).normalized()
    }

    override fun saveSettings(settings: RollSettings) {
        val normalizedSettings = settings.normalized()
        prefs.edit {
            putInt(KEY_DICE_COUNT, normalizedSettings.diceCount)
            putInt(KEY_DIE_TYPE, normalizedSettings.dieConfig.type.ordinal)
            putInt(KEY_DIE_MIN, normalizedSettings.dieConfig.minValue)
            putInt(KEY_DIE_MAX, normalizedSettings.dieConfig.maxValue)
            putBoolean(KEY_DIE_CUSTOM, normalizedSettings.dieConfig.isCustom)
            putInt(KEY_DISPLAY_MODE, normalizedSettings.displayMode.ordinal)
            putInt(KEY_ANIMATION_SPEED, normalizedSettings.animationSpeed.ordinal)
            putBoolean(KEY_SOUND, normalizedSettings.soundEnabled)
            putBoolean(KEY_VIBRATION, normalizedSettings.vibrationEnabled)
            putBoolean(KEY_SHAKE, normalizedSettings.shakeToRollEnabled)
        }
    }

    override fun loadPresets(): List<DicePreset> {
        val raw = try {
            prefs.getString(KEY_PRESETS, null)
        } catch (e: ClassCastException) {
            throw PresetStorageException(e)
        } ?: return emptyList()
        return try {
            json.decodeFromString<List<DicePreset>>(raw)
                .map { preset -> preset.copy(settings = preset.settings.normalized()) }
        } catch (e: SerializationException) {
            throw PresetStorageException(e)
        }
    }

    override fun savePreset(preset: DicePreset): Boolean {
        val normalizedPreset = preset.copy(settings = preset.settings.normalized())
        val current = loadPresets().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == normalizedPreset.id }
        if (existingIndex >= 0) {
            current[existingIndex] = normalizedPreset
        } else {
            if (!Limits.canAddPreset(current.size)) return false
            current.add(normalizedPreset)
        }
        prefs.edit { putString(KEY_PRESETS, json.encodeToString(current)) }
        return true
    }

    override fun deletePreset(presetId: String) {
        val current = loadPresets().toMutableList()
        current.removeAll { it.id == presetId }
        prefs.edit { putString(KEY_PRESETS, json.encodeToString(current)) }
    }

    private fun loadDieConfig(): DieConfig {
        val typeOrdinal = prefs.getInt(KEY_DIE_TYPE, DieType.D6.ordinal)
        val type = DieType.entries.getOrNull(typeOrdinal) ?: DieType.D6
        val isCustom = prefs.getBoolean(KEY_DIE_CUSTOM, false)
        return if (isCustom) {
            DieConfig.custom(
                prefs.getInt(KEY_DIE_MIN, Limits.MIN_CUSTOM),
                prefs.getInt(KEY_DIE_MAX, Limits.MAX_CUSTOM),
            )
        } else {
            DieConfig(
                type = type,
                minValue = 1,
                maxValue = type.sides,
                isCustom = false,
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "dice_roller_prefs"
        private const val KEY_DICE_COUNT = "dice_count"
        private const val KEY_DIE_TYPE = "die_type"
        private const val KEY_DIE_MIN = "die_min"
        private const val KEY_DIE_MAX = "die_max"
        private const val KEY_DIE_CUSTOM = "die_custom"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_ANIMATION_SPEED = "animation_speed"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_SHAKE = "shake_to_roll"
        private const val KEY_PRESETS = "dice_presets"
    }
}

class PresetStorageException(cause: Throwable) :
    IllegalStateException("Saved presets could not be read and were left unchanged.", cause)
