package com.patbaumgartner.android.diceroller.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * The only file that touches the vibrator. One short tick per roll, gated by
 * the haptics setting before it is ever called.
 *
 * The service lookup is null-safe because a device may expose no vibrator; the
 * `VIBRATE` permission is declared in the manifest and is the app's only one.
 */
internal class DiceHaptics(context: Context) {

    private val vibrator: Vibrator? = runCatching { resolveVibrator(context) }.getOrNull()

    fun tick() {
        // minSdk is 26, so VibrationEffect is always available; predefined clicks
        // need API 29, hence the branch rather than a deprecated raw duration.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            vibrator?.vibrate(VibrationEffect.createOneShot(TICK_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun resolveVibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private companion object {
        const val TICK_MS = 30L
    }
}
