package com.patbaumgartner.android.diceroller.ui

import com.patbaumgartner.android.diceroller.data.settings.PresetStorageException
import com.patbaumgartner.android.diceroller.data.settings.SettingsStore
import com.patbaumgartner.android.diceroller.domain.model.DicePreset
import com.patbaumgartner.android.diceroller.domain.model.DieConfig
import com.patbaumgartner.android.diceroller.domain.model.DieType
import com.patbaumgartner.android.diceroller.domain.model.DisplayMode
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RollerViewModelTest {

    @Test
    fun `changing die config clears locks before the next roll`() {
        val viewModel = RollerViewModel(
            FakeSettingsStore(RollSettings(dieConfig = DieConfig(DieType.D20, minValue = 1, maxValue = 20))),
        )
        viewModel.roll()
        val previousValue = requireNotNull(viewModel.currentEntry.value).values[0]
        viewModel.toggleLock(index = 0, value = previousValue)
        viewModel.updateDisplayMode(DisplayMode.NUMBERS)
        assertEquals("unrelated settings preserve current locks", mapOf(0 to previousValue), viewModel.lockedValues.value)

        viewModel.updateDieConfig(DieConfig(DieType.D4, minValue = 1, maxValue = 4))
        assertEquals("stale results are cleared after changing die type", null, viewModel.currentEntry.value)
        viewModel.roll()

        assertTrue("locks from the previous die config must be cleared", viewModel.lockedValues.value.isEmpty())
        val entry = requireNotNull(viewModel.currentEntry.value)
        assertTrue("new die results must use the D4 range", entry.values.all { it in 1..4 })
    }

    @Test
    fun `reducing dice count drops locks for removed dice`() {
        val viewModel = RollerViewModel(FakeSettingsStore(RollSettings(diceCount = 3)))
        viewModel.roll()
        val values = requireNotNull(viewModel.currentEntry.value).values
        viewModel.toggleLock(index = 0, value = values[0])
        viewModel.toggleLock(index = 2, value = values[2])

        viewModel.updateDiceCount(2)

        assertEquals("only locks for remaining dice are retained", mapOf(0 to values[0]), viewModel.lockedValues.value)
        assertEquals("stale results are cleared after changing the hand size", null, viewModel.currentEntry.value)
    }

    @Test
    fun `corrupt preset storage is reported without overwriting it`() {
        val store = FakeSettingsStore().apply { presetsCorrupt = true }
        val viewModel = RollerViewModel(store)

        assertTrue("load failure is exposed to the UI", viewModel.presetStorageError.value)
        assertEquals(
            "save reports a storage failure",
            RollerViewModel.PresetSaveResult.STORAGE_ERROR,
            viewModel.savePreset("My preset"),
        )
        viewModel.deletePreset("existing")

        assertEquals("corrupt data is not replaced by a save", 0, store.savePresetCalls)
        assertEquals("corrupt data is not replaced by a delete", 0, store.deletePresetCalls)
        assertTrue("storage error remains visible", viewModel.presetStorageError.value)
    }

    private class FakeSettingsStore(
        private var settings: RollSettings = RollSettings(),
        private val presets: MutableList<DicePreset> = mutableListOf(),
    ) : SettingsStore {
        var presetsCorrupt = false
        var savePresetCalls = 0
        var deletePresetCalls = 0

        override fun loadSettings(): RollSettings = settings

        override fun saveSettings(settings: RollSettings) {
            this.settings = settings
        }

        override fun loadPresets(): List<DicePreset> {
            if (presetsCorrupt) throw PresetStorageException(SerializationException("invalid preset data"))
            return presets.toList()
        }

        override fun savePreset(preset: DicePreset): Boolean {
            loadPresets()
            savePresetCalls++
            presets.add(preset)
            return true
        }

        override fun deletePreset(presetId: String) {
            loadPresets()
            deletePresetCalls++
            presets.removeAll { it.id == presetId }
        }
    }
}
