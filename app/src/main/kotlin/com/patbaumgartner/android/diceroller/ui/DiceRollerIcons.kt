package com.patbaumgartner.android.diceroller.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.patbaumgartner.android.diceroller.R

/**
 * Vendored Material navigation icons and an original rolling-dice icon.
 * Keeping these local avoids a dependency on the frozen `material-icons-extended` library.
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

    val RollDice: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_roll_dice)
}
