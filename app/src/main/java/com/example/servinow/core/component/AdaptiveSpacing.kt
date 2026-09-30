package com.example.servinow.core.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun screenSpacing(): Dp = if (LocalConfiguration.current.screenWidthDp < 360) 16.dp else 24.dp

@Composable
fun mapHeight(): Dp = (LocalConfiguration.current.screenHeightDp * 0.42f).coerceIn(220f, 380f).dp
