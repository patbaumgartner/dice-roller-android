package com.patbaumgartner.android.diceroller.feedback

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * A short synthesised tick played when the dice are rolled. Synthesised on
 * purpose: the app ships no media assets and never downloads any, so sound
 * stays dependency-free and works fully offline.
 *
 * Construction can fail on devices without a tone generator, so the handle is
 * deliberately nullable and every operation is a no-op when it is absent.
 */
internal class DiceSound {

    private val tone: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME)
    }.getOrNull()

    fun play() {
        tone?.startTone(ToneGenerator.TONE_PROP_ACK, TONE_DURATION_MS)
    }

    fun release() {
        tone?.release()
    }

    private companion object {
        const val TONE_VOLUME = 70
        const val TONE_DURATION_MS = 60
    }
}
