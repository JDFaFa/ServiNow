package com.example.servinow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.example.servinow.core.theme.ServiNowTheme
import com.example.servinow.features.community.CommunityRoute

// comprobando si puedo hacer commits
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ServiNowTheme { CommunityRoute() } }
    }
}
