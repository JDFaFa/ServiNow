package com.example.servinow.features.cloud

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.servinow.R
import com.example.servinow.data.cloud.ServiNowMessagingService
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun NotificationPermission() {
    val context=LocalContext.current
    var enabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var denied by remember { mutableStateOf(false) }
    fun register() { FirebaseMessaging.getInstance().token.addOnSuccessListener(ServiNowMessagingService::registerToken) }
    LifecycleResumeEffect(Unit) { enabled=NotificationManagerCompat.from(context).areNotificationsEnabled();onPauseOrDispose {} }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ granted ->
        enabled=granted;denied=!granted;if(granted)register()
    }
    OutlinedButton(onClick={
        if(Build.VERSION.SDK_INT>=33&&!enabled&&!denied)launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))
    }) { Text(stringResource(if(enabled)R.string.push_enabled else if(denied)R.string.push_settings else R.string.push_enable)) }
    if(denied)Text(stringResource(R.string.push_permission_hint))
}
