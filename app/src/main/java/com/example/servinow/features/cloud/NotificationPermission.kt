package com.example.servinow.features.cloud

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.servinow.R
import com.example.servinow.data.cloud.ServiNowMessagingService
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun NotificationPermission() {
    fun register() { FirebaseMessaging.getInstance().token.addOnSuccessListener(ServiNowMessagingService::registerToken) }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ granted -> if(granted)register() }
    OutlinedButton(onClick={if(Build.VERSION.SDK_INT>=33)launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else register()}) {
        Text(stringResource(R.string.push_enable))
    }
}
