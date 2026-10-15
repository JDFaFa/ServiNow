package com.example.servinow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import com.example.servinow.core.theme.ServiNowTheme
import com.example.servinow.features.community.CommunityRoute
import com.example.servinow.features.community.CommunityViewModel

// comprobando si puedo hacer commits
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        setContent { ServiNowTheme { CommunityRoute(viewModel) } }
    }
}
