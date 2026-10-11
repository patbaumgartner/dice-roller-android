package com.patbaumgartner.android.diceroller.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.patbaumgartner.android.diceroller.DiceRollerApp
import com.patbaumgartner.android.diceroller.ui.theme.DiceRollerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RollerViewModel by viewModels {
        RollerViewModelFactory((application as DiceRollerApp).settingsRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DiceRollerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RollerApp(viewModel)
                }
            }
        }
    }
}
