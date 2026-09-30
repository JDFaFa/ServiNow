package com.example.servinow

import android.os.Bundle
import com.example.servinow.features.explore.PublicationViewModel
import com.example.servinow.features.explore.ExploreRoute
import com.example.servinow.features.explore.ExploreViewModel
import com.example.servinow.features.recovery.RecoveryRoute
import com.example.servinow.features.recovery.RecoveryViewModel
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.servinow.features.register.RegisterRoute
import com.example.servinow.features.register.RegisterViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.servinow.core.theme.ServiNowTheme
import com.example.servinow.features.login.LoginRoute
import com.example.servinow.features.login.LoginViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        val registerViewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
        val recoveryViewModel = ViewModelProvider(this)[RecoveryViewModel::class.java]
        val exploreViewModel = ViewModelProvider(this)[ExploreViewModel::class.java]
        val publicationViewModel = ViewModelProvider(this)[PublicationViewModel::class.java]
        exploreViewModel.attachStorage(applicationContext)
        setContent {
            ServiNowTheme {
                var screen by rememberSaveable { mutableStateOf("login") }
                when (screen) {
                    "explore" -> ExploreRoute(exploreViewModel, publicationViewModel) { screen = "login" }
                    "register" -> RegisterRoute(registerViewModel) { screen = "login" }
                    "recovery" -> RecoveryRoute(recoveryViewModel) { screen = "login" }
                    else -> LoginRoute(loginViewModel,
                        onRegister = { screen = "register" },
                        onExplore = { screen = "explore" },
                        onRecover = { recoveryViewModel.restart(); screen = "recovery" })
                }
            }
        }
    }
}
