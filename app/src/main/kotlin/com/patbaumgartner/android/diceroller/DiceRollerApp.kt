package com.patbaumgartner.android.diceroller

import android.app.Application
import com.patbaumgartner.android.diceroller.data.settings.SettingsRepository

class DiceRollerApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }
}