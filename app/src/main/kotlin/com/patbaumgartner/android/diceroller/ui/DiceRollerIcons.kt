package com.patbaumgartner.android.diceroller.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.patbaumgartner.android.diceroller.R

/**
 * The Material icons this app draws, vendored as vector drawables. `material-icons-extended` is
 * frozen at 1.7.8 while the rest of Compose moves on, and it shipped thousands of icons to use
 * four. The drawables were generated from that library, so the geometry is unchanged.
 */
internal object DiceRollerIcons {

    val ArrowBack: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_arrow_back)

    val DeleteOutline: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_delete_outline)

    val MoreVert: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_more_horiz)

    val Settings: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_settings)
}
