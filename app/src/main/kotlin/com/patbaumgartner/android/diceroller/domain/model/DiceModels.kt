package com.patbaumgartner.android.diceroller.domain.model

import kotlinx.serialization.Serializable

enum class DieType(val sides: Int, val label: String) {
    D4(4, "D4"),
    D6(6, "D6"),
    D8(8, "D8"),
    D10(10, "D10"),
    D12(12, "D12"),
    D20(20, "D20"),
    D100(100, "D100"),
    CUSTOM(0, "Custom"),
}

@Serializable
data class DieConfig(
    val type: DieType = DieType.D6,
    val minValue: Int = 1,
    val maxValue: Int = 6,
    val isCustom: Boolean = false,
) {
    fun range(): IntRange = minValue..maxValue
}

@Serializable
data class RollEntry(
    val id: Long = System.currentTimeMillis(),
    val values: List<Int>,
    val config: DieConfig,
    val lockedIndices: Set<Int> = emptySet(),
)

@Serializable
enum class DisplayMode { DOTS, NUMBERS }

@Serializable
enum class AnimationSpeed(val durationMs: Long) {
    OFF(0L),
    SLOW(1500L),
    NORMAL(800L),
    FAST(300L),
}

@Serializable
data class RollSettings(
    val diceCount: Int = 2,
    val dieConfig: DieConfig = DieConfig(),
    val displayMode: DisplayMode = DisplayMode.DOTS,
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val shakeToRollEnabled: Boolean = false,
)

@Serializable
data class DicePreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val settings: RollSettings,
    val createdAt: Long = System.currentTimeMillis(),
)
