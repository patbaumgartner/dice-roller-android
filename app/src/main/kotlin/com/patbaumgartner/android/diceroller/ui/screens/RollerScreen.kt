package com.patbaumgartner.android.diceroller.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patbaumgartner.android.diceroller.R
import com.patbaumgartner.android.diceroller.domain.model.DisplayMode
import com.patbaumgartner.android.diceroller.domain.model.RollEntry
import com.patbaumgartner.android.diceroller.domain.model.RollSettings
import com.patbaumgartner.android.diceroller.ui.components.DieFace
import com.patbaumgartner.android.diceroller.ui.theme.DiceAmber
import com.patbaumgartner.android.diceroller.ui.theme.DiceBlue
import com.patbaumgartner.android.diceroller.ui.theme.DiceGreen
import com.patbaumgartner.android.diceroller.ui.theme.DiceIndigo
import com.patbaumgartner.android.diceroller.ui.theme.DiceOrange
import com.patbaumgartner.android.diceroller.ui.theme.DicePink
import com.patbaumgartner.android.diceroller.ui.theme.DicePurple
import com.patbaumgartner.android.diceroller.ui.theme.DiceRed
import com.patbaumgartner.android.diceroller.ui.theme.DiceTeal
import com.patbaumgartner.android.diceroller.ui.DiceRollerIcons

private val diceColors = listOf(
    DiceRed, DiceBlue, DiceGreen, DicePurple, DiceOrange,
    DiceTeal, DicePink, DiceAmber, DiceIndigo,
)

/**
 * The whole app on one calm page: dice in the middle, total above, one roll
 * control below. Every setup control lives in Settings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RollerScreen(
    settings: RollSettings,
    currentEntry: RollEntry?,
    lockedValues: Map<Int, Int>,
    onRoll: () -> Unit,
    onToggleLock: (Int, Int) -> Unit,
    onOpenPresets: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val animationDurationMs = settings.animationSpeed.durationMs.toInt()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RollerHeader(
            onOpenPresets = onOpenPresets,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
        )

        Spacer(Modifier.height(20.dp))

        AnimatedContent(
            targetState = currentEntry,
            transitionSpec = {
                fadeIn(tween(animationDurationMs)) togetherWith fadeOut(tween(animationDurationMs))
            },
            label = "total",
        ) { entry ->
            Text(
                text = if (entry == null) {
                    stringResource(R.string.roll_prompt)
                } else {
                    stringResource(R.string.total_short_label, entry.values.sum())
                },
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        AnimatedContent(
            targetState = currentEntry?.values ?: emptyList(),
            transitionSpec = {
                fadeIn(tween(animationDurationMs)) togetherWith fadeOut(tween(animationDurationMs))
            },
            label = "breakdown",
        ) { values ->
            Text(
                text = values.joinToString("  +  "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(28.dp))
        DiceGrid(
            entry = currentEntry,
            diceCount = settings.diceCount,
            lockedValues = lockedValues,
            showPips = settings.displayMode == DisplayMode.DOTS,
            onToggleLock = onToggleLock,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 28.dp),
        )
        Spacer(Modifier.height(16.dp))

        RollControl(onRoll = onRoll)
        Text(
            text = stringResource(if (lockedValues.isEmpty()) R.string.hint_roll else R.string.hint_lock),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (lockedValues.isNotEmpty()) {
                Text(
                    text = pluralStringResource(
                        R.plurals.locked_hint,
                        lockedValues.size,
                        lockedValues.size,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RollerHeader(
    onOpenPresets: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    DiceRollerIcons.MoreVert,
                    contentDescription = stringResource(R.string.cd_menu),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.screen_presets)) },
                    onClick = { menuExpanded = false; onOpenPresets() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.screen_history)) },
                    onClick = { menuExpanded = false; onOpenHistory() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.screen_settings)) },
                    leadingIcon = { Icon(DiceRollerIcons.Settings, contentDescription = null) },
                    onClick = { menuExpanded = false; onOpenSettings() },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiceGrid(
    entry: RollEntry?,
    diceCount: Int,
    lockedValues: Map<Int, Int>,
    showPips: Boolean,
    onToggleLock: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = entry?.values?.size ?: diceCount
    val dieSize = dieSizeFor(count)
    FlowRow(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (entry == null) {
            repeat(count) { index ->
                val lockedValue = lockedValues[index]
                DieFace(
                    value = lockedValue ?: 1,
                    accent = diceColors[index % diceColors.size],
                    isLocked = lockedValue != null,
                    showPips = showPips,
                    modifier = Modifier.size(dieSize),
                    onClick = { lockedValue?.let { onToggleLock(index, it) } },
                )
            }
            return@FlowRow
        }
        entry.values.forEachIndexed { index, value ->
            DieFace(
                value = value,
                accent = diceColors[index % diceColors.size],
                isLocked = index in lockedValues,
                showPips = showPips,
                modifier = Modifier.size(dieSize),
                onClick = { onToggleLock(index, value) },
            )
        }
    }
}

/** Larger dice for small hands of dice; gracefully smaller grids for 40. */
private fun dieSizeFor(count: Int) = when {
    count <= 4 -> 96.dp
    count <= 9 -> 84.dp
    count <= 12 -> 72.dp
    count <= 20 -> 60.dp
    else -> 60.dp
}

@Composable
private fun RollControl(onRoll: () -> Unit) {
    Surface(
        onClick = onRoll,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .size(120.dp)
            .semantics { role = Role.Button },
        shadowElevation = 3.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = DiceRollerIcons.RollDice,
                contentDescription = stringResource(R.string.action_roll),
                modifier = Modifier.size(76.dp),
            )
        }
    }
}
