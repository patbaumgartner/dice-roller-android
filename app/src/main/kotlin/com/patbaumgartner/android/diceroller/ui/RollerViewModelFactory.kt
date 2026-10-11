package com.patbaumgartner.android.diceroller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.patbaumgartner.android.diceroller.data.settings.SettingsStore

class RollerViewModelFactory(
    private val settingsRepository: SettingsStore,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RollerViewModel::class.java)) {
            return RollerViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
