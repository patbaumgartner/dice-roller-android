package com.patbaumgartner.android.diceroller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patbaumgartner.android.diceroller.R
import com.patbaumgartner.android.diceroller.domain.model.DieConfig
import com.patbaumgartner.android.diceroller.domain.model.DieType
import com.patbaumgartner.android.diceroller.domain.model.RollEntry
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import com.patbaumgartner.android.diceroller.ui.RollerViewModel
import com.patbaumgartner.android.diceroller.ui.theme.DiceAmber
import com.patbaumgartner.android.diceroller.ui.theme.DiceBlue
import com.patbaumgartner.android.diceroller.ui.theme.DiceGreen
import com.patbaumgartner.android.diceroller.ui.theme.DiceIndigo
import com.patbaumgartner.android.diceroller.ui.theme.DiceOrange
import com.patbaumgartner.android.diceroller.ui.theme.DicePink
import com.patbaumgartner.android.diceroller.ui.theme.DicePurple
import com.patbaumgartner.android.diceroller.ui.theme.DiceRed
import com.patbaumgartner.android.diceroller.ui.theme.DiceTeal

private val diceColors = listOf(
    DiceRed, DiceBlue, DiceGreen, DicePurple, DiceOrange,
    DiceTeal, DicePink, DiceAmber, DiceIndigo,
)

@Composable
fun RollerScreen(
    settings: RollSettings,
    currentEntry: RollEntry?,
    lockedValues: Map<Int, Int>,
    history: List<RollEntry>,
    onRoll: () -> Unit,
    onToggleLock: (Int, Int) -> Unit,
    onClearLocked: () -> Unit,
    onReset: () -> Unit,
    onUpdateDiceCount: (Int) -> Unit,
    onUpdateDieConfig: (DieConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DiceCountSelector(
            count = settings.diceCount,
            onCountChange = onUpdateDiceCount,
        )
        DieTypeSelector(
            config = settings.dieConfig,
            onConfigChange = onUpdateDieConfig,
        )
        TotalDisplay(entry = currentEntry)
        DiceGrid(
            entry = currentEntry,
            lockedValues = lockedValues,
            onToggleLock = onToggleLock,
        )
        RollControls(
            onRoll = onRoll,
            onReset = onReset,
            hasLocked = lockedValues.isNotEmpty(),
            onClearLocked = onClearLocked,
        )
        RollHistory(history = history)
    }
}

@Composable
private fun DiceCountSelector(count: Int, onCountChange: (Int) -> Unit) {
    Column {
        Text(stringResource(R.string.dice_count_label, count), style = MaterialTheme.typography.titleMedium)
        Slider(
            value = count.toFloat(),
            onValueChange = { onCountChange(it.toInt()) },
            valueRange = RollerViewModel.MIN_DICE.toFloat()..RollerViewModel.MAX_DICE.toFloat(),
            steps = RollerViewModel.MAX_DICE - RollerViewModel.MIN_DICE - 1,
        )
    }
}

@Composable
private fun DieTypeSelector(config: DieConfig, onConfigChange: (DieConfig) -> Unit) {
    Column {
        Text(stringResource(R.string.die_type_label), style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DieType.entries.filter { it != DieType.CUSTOM }.forEach { type ->
                FilterChip(
                    selected = config.type == type && !config.isCustom,
                    onClick = {
                        onConfigChange(
                            DieConfig(type = type, minValue = 1, maxValue = type.sides, isCustom = false),
                        )
                    },
                    label = { Text(type.label) },
                )
            }
        }
        if (config.isCustom) {
            Text(
                stringResource(R.string.custom_range_label, config.minValue, config.maxValue),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TotalDisplay(entry: RollEntry?) {
    val total = entry?.values?.sum() ?: 0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = entry?.let { stringResource(R.string.total_label, total) }
                ?: stringResource(R.string.roll_prompt),
            style = MaterialTheme.typography.displayLarge,
        )
    }
}

@Composable
private fun DiceGrid(
    entry: RollEntry?,
    lockedValues: Map<Int, Int>,
    onToggleLock: (Int, Int) -> Unit,
) {
    if (entry == null) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        entry.values.forEachIndexed { index, value ->
            DieView(
                value = value,
                isLocked = index in lockedValues,
                color = diceColors[index % diceColors.size],
                onClick = { onToggleLock(index, value) },
            )
        }
    }
}

@Composable
private fun DieView(value: Int, isLocked: Boolean, color: Color, onClick: () -> Unit) {
    val dieDescription = stringResource(R.string.cd_die_result, value)
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(
                width = if (isLocked) 3.dp else 1.dp,
                color = if (isLocked) color else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = dieDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        if (isLocked) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp),
                tint = color,
            )
        }
    }
}

@Composable
private fun RollControls(
    onRoll: () -> Unit,
    onReset: () -> Unit,
    hasLocked: Boolean,
    onClearLocked: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onRoll,
                modifier = Modifier.weight(2f),
            ) {
                Text(stringResource(R.string.action_roll), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_reset))
            }
        }
        if (hasLocked) {
            TextButton(onClick = onClearLocked) {
                Text(stringResource(R.string.action_clear_locked))
            }
        }
    }
}

@Composable
private fun RollHistory(history: List<RollEntry>) {
    if (history.isEmpty()) return
    Column {
        Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleMedium)
        history.take(5).forEach { entry ->
            Text(
                text = "${entry.config.type.label}: ${entry.values.joinToString(", ")} (${entry.values.sum()})",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
