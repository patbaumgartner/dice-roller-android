package com.patbaumgartner.android.diceroller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patbaumgartner.android.diceroller.R

/**
 * A rounded die with an opaque top-to-bottom gradient, pips for faces 1-6 and the
 * numeral for anything above. With [showPips] false the classic faces also
 * render as numerals. Tapping toggles the lock.
 */
@Composable
fun DieFace(
    value: Int,
    accent: Color,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    showPips: Boolean = true,
    onClick: () -> Unit = {},
) {
    val dieDescription = stringResource(R.string.cd_die_result, value)
    val lockDescription = stringResource(
        if (isLocked) R.string.cd_die_locked else R.string.cd_die_unlocked,
    )
    val faceTop = if (isLocked) accent.darken() else accent
    val faceBottom = faceTop.darken()

    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(listOf(faceTop, faceBottom)),
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = dieDescription
                stateDescription = lockDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        PipsOrNumber(value = value, ink = faceInk(accent), showPips = showPips)
        if (isLocked) {
            Box(Modifier.matchParentSize()) {
                LockBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    tint = faceInk(accent).copy(alpha = 0.9f),
                )
            }
        }
    }
}

private fun Color.darken(): Color = Color(
    red = red * 0.78f,
    green = green * 0.78f,
    blue = blue * 0.78f,
    alpha = alpha,
)

/** Ink colour: dark on light accents, white on saturated accents. */
private fun faceInk(accent: Color): Color {
    val luminance = 0.2126f * accent.red + 0.7152f * accent.green + 0.0722f * accent.blue
    return if (luminance > 0.62f) Color(0xFF14181A) else Color.White
}

/**
 * Standard western die pip positions on a unit 3x3 grid, matching real dice:
 * opposite faces sum to seven and each face follows the familiar pattern.
 * Coordinates are (column, row) with (0,0) top-left, read left-to-right,
 * top-to-bottom per row.
 */
internal val StandardPipPositions: Map<Int, Set<Pair<Int, Int>>> = mapOf(
    1 to setOf(1 to 1),
    2 to setOf(0 to 0, 2 to 2),
    3 to setOf(0 to 0, 1 to 1, 2 to 2),
    4 to setOf(0 to 0, 2 to 0, 0 to 2, 2 to 2),
    5 to setOf(0 to 0, 2 to 0, 1 to 1, 0 to 2, 2 to 2),
    6 to setOf(0 to 0, 2 to 0, 0 to 1, 2 to 1, 0 to 2, 2 to 2),
)

internal fun pipPositions(value: Int): Set<Pair<Int, Int>> =
    StandardPipPositions[value] ?: emptySet()

@Composable
private fun PipsOrNumber(value: Int, ink: Color, showPips: Boolean) {
    val positions = if (showPips) pipPositions(value) else emptySet()
    if (positions.isEmpty()) {
        Text(
            text = value.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ink,
        )
        return
    }
    PipLayout(positions = positions, ink = ink)
}

/**
 * Pips are laid out on a real 3x3 grid spread across the whole face — like a
 * physical die — instead of clustered in the middle. Invisible cells keep
 * their grid slot so filled pips land on the classic positions.
 */
@Composable
private fun PipLayout(positions: Set<Pair<Int, Int>>, ink: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PipGap),
    ) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(PipGap)) {
                repeat(3) { col ->
                    Box(
                        modifier = Modifier
                            .size(PipSize)
                            .clip(CircleShape)
                            .background(if ((col to row) in positions) ink else Color.Transparent),
                    )
                }
            }
        }
    }
}

private val PipSize = 9.dp
private val PipGap = 6.dp

@Composable
private fun LockBadge(modifier: Modifier = Modifier, tint: Color) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(tint),
    )
}
