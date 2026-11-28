package com.patbaumgartner.android.diceroller.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.patbaumgartner.android.diceroller.domain.model.DicePreset
import com.patbaumgartner.android.diceroller.domain.model.Limits
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val repository = SettingsRepository(context)
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var savedPreferences: Map<String, *>

    @Before
    fun backUpPreferences() {
        savedPreferences = preferences.all.toMap()
    }

    @After
    fun restorePreferences() {
        val editor = preferences.edit().clear()
        savedPreferences.forEach { (key, value) -> editor.putValue(key, value) }
        check(editor.commit()) { "test preferences must be restored" }
    }

    @Test
    fun persistedDiceCountsAreNormalizedOnLoadAndSave() {
        invalidCounts.forEach { (storedCount, expectedCount) ->
            check(preferences.edit().putInt(KEY_DICE_COUNT, storedCount).commit())
            assertEquals(
                "raw persisted count $storedCount must be normalized on load",
                expectedCount,
                repository.loadSettings().diceCount,
            )

            repository.saveSettings(RollSettings(diceCount = storedCount))

            assertEquals(
                "count $storedCount must be normalized before it is saved",
                expectedCount,
                preferences.getInt(KEY_DICE_COUNT, Int.MIN_VALUE),
            )
        }
    }

    @Test
    fun presetDiceCountsAreNormalizedOnDecodeAndWriteWithoutRewritingOnRead() {
        invalidCounts.forEach { (storedCount, expectedCount) ->
            val preset = DicePreset(
                id = "invalid-count",
                name = "Invalid count",
                settings = RollSettings(diceCount = storedCount),
            )
            val originalJson = json.encodeToString(listOf(preset))
            check(preferences.edit().putString(KEY_PRESETS, originalJson).commit())

            val loaded = repository.loadPresets().single()
            assertEquals(
                "decoded preset count $storedCount must be normalized",
                expectedCount,
                loaded.settings.diceCount,
            )
            assertEquals(
                "reading semantic corruption must not rewrite the original JSON",
                originalJson,
                preferences.getString(KEY_PRESETS, null),
            )

            assertTrue("preset fixture must be saved", repository.savePreset(preset))
            val storedJson = preferences.getString(KEY_PRESETS, null)
            val storedPreset = json.decodeFromString<List<DicePreset>>(requireNotNull(storedJson)).single()
            assertEquals(
                "preset count $storedCount must be normalized before it is written",
                expectedCount,
                storedPreset.settings.diceCount,
            )
        }
    }

    private fun SharedPreferences.Editor.putValue(key: String, value: Any?): SharedPreferences.Editor = when (value) {
        is Boolean -> putBoolean(key, value)
        is Float -> putFloat(key, value)
        is Int -> putInt(key, value)
        is Long -> putLong(key, value)
        is String -> putString(key, value)
        is Set<*> -> {
            @Suppress("UNCHECKED_CAST")
            putStringSet(key, value as Set<String>)
        }
        else -> remove(key)
    }

    companion object {
        private const val PREFS_NAME = "dice_roller_prefs"
        private const val KEY_DICE_COUNT = "dice_count"
        private const val KEY_PRESETS = "dice_presets"
        private val invalidCounts = listOf(
            -1 to Limits.MIN_DICE,
            0 to Limits.MIN_DICE,
            41 to Limits.MAX_DICE,
            Int.MAX_VALUE to Limits.MAX_DICE,
        )
    }
}
