package com.patbaumgartner.android.diceroller.domain.usecase

import com.patbaumgartner.android.diceroller.domain.model.DieConfig
import com.patbaumgartner.android.diceroller.domain.model.DieType
import com.patbaumgartner.android.diceroller.domain.model.RollEntry
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import kotlin.random.Random

class RollDiceUseCase(private val random: Random = Random.Default) {

    fun roll(settings: RollSettings, lockedValues: Map<Int, Int> = emptyMap()): RollEntry {
        val config = settings.dieConfig
        val lower = minOf(config.minValue, config.maxValue)
        val upper = maxOf(config.minValue, config.maxValue)
        val values = List(settings.diceCount) { index ->
            lockedValues[index] ?: random.nextInt(lower, upper + 1)
        }
        return RollEntry(values = values, config = config)
    }

    companion object {
        fun validateCustomRange(minValue: Int, maxValue: Int): Boolean =
            minValue in MIN_CUSTOM..MAX_CUSTOM && maxValue in MIN_CUSTOM..MAX_CUSTOM && minValue <= maxValue

        const val MIN_CUSTOM = 0
        const val MAX_CUSTOM = 100
    }
}
