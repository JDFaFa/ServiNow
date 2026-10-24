package com.example.servinow.core.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.example.servinow.R

@Composable
fun PasswordVisibilityButton(visible: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
            contentDescription = if (visible) androidx.compose.ui.res.stringResource(R.string.ui_hide_password_048b) else androidx.compose.ui.res.stringResource(R.string.ui_show_password_8b9c),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
