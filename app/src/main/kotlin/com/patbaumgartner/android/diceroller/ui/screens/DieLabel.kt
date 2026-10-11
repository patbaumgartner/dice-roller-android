package com.patbaumgartner.android.diceroller.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.patbaumgartner.android.diceroller.R
import com.patbaumgartner.android.diceroller.domain.model.DieConfig

/** Human-readable label for a die config: its type, or the custom range. */
@Composable
internal fun dieLabel(config: DieConfig): String =
    if (config.isCustom) {
        stringResource(R.string.custom_range_label, config.minValue, config.maxValue)
    } else {
        config.type.label
    }
