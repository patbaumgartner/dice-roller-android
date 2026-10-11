package com.patbaumgartner.android.diceroller.domain.model

/**
 * Single source of truth for the product's caps, shared by the ViewModel and
 * the repository so both stay honest and testable on the JVM.
 */
object Limits {
    const val MIN_DICE = 1
    const val MAX_DICE = 40
    const val MAX_PRESETS = 10
    const val HISTORY_LIMIT = 50

    /** Bounds of an editable custom die range, inclusive. */
    const val MIN_CUSTOM = 0
    const val MAX_CUSTOM = 100

    fun canAddPreset(currentCount: Int): Boolean = currentCount < MAX_PRESETS

    fun cappedHistory(entries: List<RollEntry>): List<RollEntry> = entries.take(HISTORY_LIMIT)

    fun cappedPresets(presets: List<DicePreset>): List<DicePreset> = presets.take(MAX_PRESETS)
}
