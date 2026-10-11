package com.patbaumgartner.android.diceroller.domain.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `default settings roll two standard D6`() {
        val settings = RollSettings()
        assertEquals("default dice count", 2, settings.diceCount)
        assertEquals("default die is D6", DieType.D6, settings.dieConfig.type)
        assertEquals("default range starts at 1", 1, settings.dieConfig.minValue)
        assertEquals("default range ends at 6", 6, settings.dieConfig.maxValue)
        assertEquals("dots is the default display", DisplayMode.DOTS, settings.displayMode)
        assertEquals("normal animation by default", AnimationSpeed.NORMAL, settings.animationSpeed)
        assertTrue("sound on by default", settings.soundEnabled)
        assertTrue("vibration on by default", settings.vibrationEnabled)
        assertFalse("shake off by default", settings.shakeToRollEnabled)
    }

    @Test
    fun `die types carry correct side counts and labels`() {
        assertEquals(4, DieType.D4.sides)
        assertEquals(6, DieType.D6.sides)
        assertEquals(8, DieType.D8.sides)
        assertEquals(10, DieType.D10.sides)
        assertEquals(12, DieType.D12.sides)
        assertEquals(20, DieType.D20.sides)
        assertEquals(100, DieType.D100.sides)
        assertEquals(0, DieType.CUSTOM.sides)
        assertEquals("D100", DieType.D100.label)
    }

    @Test
    fun `roll settings serialization round trip preserves everything`() {
        val settings = RollSettings(
            diceCount = 7,
            dieConfig = DieConfig(DieType.D12),
            displayMode = DisplayMode.NUMBERS,
            animationSpeed = AnimationSpeed.FAST,
            soundEnabled = false,
            vibrationEnabled = false,
            shakeToRollEnabled = true,
        )
        val decoded = json.decodeFromString<RollSettings>(json.encodeToString(settings))
        assertEquals("round trip must preserve settings", settings, decoded)
    }

    @Test
    fun `preset serialization round trip preserves settings`() {
        val preset = DicePreset(
            name = "Test preset",
            settings = RollSettings(diceCount = 6, dieConfig = DieConfig(DieType.D20)),
        )
        val decoded = json.decodeFromString<DicePreset>(json.encodeToString(preset))
        assertEquals("round trip must preserve the preset", preset, decoded)
        assertTrue("preset must have an id", decoded.id.isNotBlank())
    }

    @Test
    fun `presets with distinct ids are distinct`() {
        val a = DicePreset(name = "a", settings = RollSettings())
        val b = DicePreset(name = "b", settings = RollSettings())
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun `roll entry holds values config and locked indices`() {
        val config = DieConfig(DieType.D6)
        val entry = RollEntry(values = listOf(1, 2, 3), config = config, lockedIndices = setOf(0))
        assertEquals(3, entry.values.size)
        assertEquals(config, entry.config)
        assertEquals(setOf(0), entry.lockedIndices)
    }

    @Test
    fun `animation speeds have ordered durations`() {
        assertTrue("OFF must be fastest", AnimationSpeed.OFF.durationMs < AnimationSpeed.FAST.durationMs)
        assertTrue(AnimationSpeed.FAST.durationMs < AnimationSpeed.NORMAL.durationMs)
        assertTrue(AnimationSpeed.NORMAL.durationMs < AnimationSpeed.SLOW.durationMs)
    }

    @Test
    fun `display mode has exactly dots and numbers`() {
        assertEquals(setOf("DOTS", "NUMBERS"), DisplayMode.entries.map { it.name }.toSet())
    }

    @Test
    fun `ignore unknown keys keeps forward compatibility`() {
        val jsonWithExtra = """{"diceCount":3,"unknownField":42,"another":true}"""
        val decoded = json.decodeFromString<RollSettings>(jsonWithExtra)
        assertEquals("unknown keys must be ignored", 3, decoded.diceCount)
    }

    @Test
    fun `preset equality relies on id`() {
        val settings = RollSettings()
        val a = DicePreset(id = "same", name = "a", settings = settings)
        val b = DicePreset(id = "same", name = "b", settings = settings)
        assertNotEquals(a, b)
        assertEquals(a.copy(name = "a"), a)
    }
}
