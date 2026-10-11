package com.patbaumgartner.android.diceroller.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The caps live in the domain layer so they are testable on the JVM. */
class LimitsTest {

    @Test
    fun `history and preset limits are the documented values`() {
        assertEquals("history cap", 50, Limits.HISTORY_LIMIT)
        assertEquals("preset cap", 10, Limits.MAX_PRESETS)
        assertEquals("min dice", 1, Limits.MIN_DICE)
        assertEquals("max dice", 40, Limits.MAX_DICE)
        assertEquals("custom lower bound", 0, Limits.MIN_CUSTOM)
        assertEquals("custom upper bound", 100, Limits.MAX_CUSTOM)
    }

    @Test
    fun `canAddPreset is true below the cap and false at it`() {
        assertTrue("empty list can add", Limits.canAddPreset(0))
        assertTrue("nine presets can add one more", Limits.canAddPreset(9))
        assertFalse("ten presets is the cap", Limits.canAddPreset(10))
    }

    @Test
    fun `cappedPresets keeps only the first ten in list order`() {
        val presets = (1..15).map { i ->
            DicePreset(id = "$i", name = "p$i", settings = RollSettings())
        }
        val capped = Limits.cappedPresets(presets)
        assertEquals(10, capped.size)
        assertEquals("first is kept", "1", capped.first().id)
        assertEquals("tenth kept, eleventh dropped", "10", capped.last().id)
        assertFalse("eleventh must be dropped", capped.any { it.id == "11" })
    }

    @Test
    fun `cappedHistory keeps the newest fifty of a newest-first list`() {
        // Callers (the ViewModel) prepend the newest entry, so index 0 is newest.
        val entries = (1..60).map { i ->
            RollEntry(values = listOf(61 - i), config = DieConfig())
        }
        val capped = Limits.cappedHistory(entries)
        assertEquals(50, capped.size)
        assertEquals("newest entry first", listOf(60), capped.first().values)
        assertEquals("oldest kept entry", listOf(11), capped.last().values)
    }

    @Test
    fun `cap with fewer items returns all of them`() {
        assertEquals(3, Limits.cappedPresets((1..3).map { i ->
            DicePreset(id = "$i", name = "p$i", settings = RollSettings())
        }).size)
        assertEquals(5, Limits.cappedHistory((1..5).map { i ->
            RollEntry(values = listOf(i), config = DieConfig())
        }).size)
    }
}
