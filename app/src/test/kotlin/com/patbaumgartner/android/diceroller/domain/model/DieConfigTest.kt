package com.patbaumgartner.android.diceroller.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DieConfigTest {

    @Test
    fun `standard die defaults span one to sides`() {
        DieType.entries.filter { it != DieType.CUSTOM }.forEach { type ->
            val config = DieConfig(type = type, minValue = 1, maxValue = type.sides)
            assertEquals("wrong range for ${type.label}", 1, config.range().first)
            assertEquals("wrong range for ${type.label}", type.sides, config.range().last)
        }
    }

    @Test
    fun `custom range is preserved`() {
        val config = DieConfig(type = DieType.CUSTOM, minValue = 0, maxValue = 100, isCustom = true)
        assertEquals(0, config.range().first)
        assertEquals(100, config.range().last)
        assertTrue(config.isCustom)
    }

    @Test
    fun `serialization round trip preserves config`() {
        val config = DieConfig(type = DieType.CUSTOM, minValue = 0, maxValue = 9, isCustom = true)
        val json = kotlinx.serialization.json.Json
        val decoded = json.decodeFromString<DieConfig>(json.encodeToString(config))
        assertEquals("round trip must preserve the config", config, decoded)
    }

    @Test
    fun `normalized range orders inverted bounds`() {
        val config = DieConfig(type = DieType.CUSTOM, minValue = 10, maxValue = 5, isCustom = true)
        assertEquals("inverted bounds are put in order", 5..10, config.normalizedRange())
    }

    @Test
    fun `normalized range clamps to the supported custom bounds`() {
        val tooWide = DieConfig(type = DieType.CUSTOM, minValue = -5, maxValue = 120, isCustom = true)
        assertEquals("out-of-support bounds are clamped", 0..100, tooWide.normalizedRange())
    }

    @Test
    fun `normalized range keeps a single value range valid`() {
        val single = DieConfig(type = DieType.CUSTOM, minValue = 7, maxValue = 7, isCustom = true)
        assertEquals("a single value range stays itself", 7..7, single.normalizedRange())
    }

    @Test
    fun `custom factory orders and clamps the bounds`() {
        val config = DieConfig.custom(minValue = 100, maxValue = 0)
        assertTrue("factory marks the config custom", config.isCustom)
        assertEquals("factory uses the custom type", DieType.CUSTOM, config.type)
        assertEquals("factory orders the bounds", 0, config.minValue)
        assertEquals("factory orders the bounds", 100, config.maxValue)

        val clamped = DieConfig.custom(minValue = -20, maxValue = 999)
        assertEquals("factory clamps the lower bound", 0, clamped.minValue)
        assertEquals("factory clamps the upper bound", 100, clamped.maxValue)
    }
}
