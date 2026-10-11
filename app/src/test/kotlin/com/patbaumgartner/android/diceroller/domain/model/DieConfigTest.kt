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
}
