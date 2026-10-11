package com.patbaumgartner.android.diceroller.domain.usecase

import com.patbaumgartner.android.diceroller.domain.model.DieConfig
import com.patbaumgartner.android.diceroller.domain.model.DieType
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RollDiceUseCaseTest {

    private val useCase = RollDiceUseCase()

    @Test
    fun `rolls the requested number of dice`() {
        val settings = RollSettings(diceCount = 5, dieConfig = DieConfig(DieType.D6))
        val entry = useCase.roll(settings)
        assertEquals("one value per die", 5, entry.values.size)
    }

    @Test
    fun `standard die values stay within range`() {
        repeat(200) {
            val entry = useCase.roll(RollSettings(diceCount = 3, dieConfig = DieConfig(DieType.D20)))
            entry.values.forEach { value ->
                assertTrue("value $value outside 1..20", value in 1..20)
            }
        }
    }

    @Test
    fun `custom zero based range includes zero`() {
        val config = DieConfig(type = DieType.CUSTOM, minValue = 0, maxValue = 1, isCustom = true)
        val seen = mutableSetOf<Int>()
        repeat(200) {
            val entry = useCase.roll(RollSettings(diceCount = 2, dieConfig = config))
            entry.values.forEach { value ->
                assertTrue("value outside 0..1", value in 0..1)
                seen.add(value)
            }
        }
        assertEquals("both faces should appear", setOf(0, 1), seen)
    }

    @Test
    fun `locked dice keep their value and only free dice roll`() {
        val settings = RollSettings(diceCount = 4, dieConfig = DieConfig(DieType.D6))
        val entry = useCase.roll(settings, lockedValues = mapOf(0 to 6, 3 to 2))
        assertEquals("die 0 locked at 6", 6, entry.values[0])
        assertEquals("die 3 locked at 2", 2, entry.values[3])
        listOf(1, 2).forEach { index ->
            assertTrue("die $index should roll in 1..6", entry.values[index] in 1..6)
        }
    }

    @Test
    fun `locked values are never regenerated`() {
        repeat(20) {
            val entry = useCase.roll(
                RollSettings(diceCount = 2, dieConfig = DieConfig(DieType.D6)),
                lockedValues = mapOf(1 to 6),
            )
            assertEquals("locked die must stay at 6", 6, entry.values[1])
        }
    }

    @Test
    fun `seeded random is deterministic`() {
        val settings = RollSettings(diceCount = 3, dieConfig = DieConfig(DieType.D6))
        val first = RollDiceUseCase(Random(42)).roll(settings)
        val second = RollDiceUseCase(Random(42)).roll(settings)
        assertEquals("same seed, same roll", first.values, second.values)
    }

    @Test
    fun `custom range validation accepts 0 to 100`() {
        assertTrue("full custom range must be accepted", RollDiceUseCase.validateCustomRange(0, 100))
        assertTrue("single-value range must be accepted", RollDiceUseCase.validateCustomRange(5, 5))
    }

    @Test
    fun `custom range validation rejects inverted and out-of-bounds ranges`() {
        assertFalse("inverted range must be rejected", RollDiceUseCase.validateCustomRange(10, 5))
        assertFalse("negative min must be rejected", RollDiceUseCase.validateCustomRange(-1, 10))
        assertFalse("max above 100 must be rejected", RollDiceUseCase.validateCustomRange(0, 101))
    }
}
