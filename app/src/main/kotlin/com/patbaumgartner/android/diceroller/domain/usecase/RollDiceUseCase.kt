package com.patbaumgartner.android.diceroller.domain.usecase

import com.patbaumgartner.android.diceroller.domain.model.Limits
import com.patbaumgartner.android.diceroller.domain.model.RollEntry
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import kotlin.random.Random

class RollDiceUseCase(private val random: Random = Random.Default) {

    fun roll(settings: RollSettings, lockedValues: Map<Int, Int> = emptyMap()): RollEntry {
        val config = settings.dieConfig
        val range = config.normalizedRange()
        val values = List(settings.diceCount) { index ->
            lockedValues[index] ?: random.nextInt(range.first, range.last + 1)
        }
        return RollEntry(values = values, config = config)
    }

    companion object {
        fun validateCustomRange(minValue: Int, maxValue: Int): Boolean =
            minValue in MIN_CUSTOM..MAX_CUSTOM && maxValue in MIN_CUSTOM..MAX_CUSTOM && minValue <= maxValue

        const val MIN_CUSTOM = Limits.MIN_CUSTOM
        const val MAX_CUSTOM = Limits.MAX_CUSTOM
    }
}
