package com.example.servinow.core.component

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.servinow.R
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One foreground request; releases listeners on success, timeout and disposal. */
@Composable
internal fun CurrentLocationButton(modifier: Modifier = Modifier, onLocation: (Double, Double) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callback by rememberUpdatedState(onLocation)
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var settings by remember { mutableStateOf(false) }
    fun locate() {
        if (busy) return
        scope.launch {
            busy = true
            try {
                val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { manager.isProviderEnabled(it) }
                if (providers.isEmpty()) { settings = true; error = R.string.location_disabled; return@launch }
                val result = withTimeoutOrNull(20000) {
                    suspendCancellableCoroutine<Location> { continuation ->
                        val listener = object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                if (continuation.isActive) { manager.removeUpdates(this); continuation.resume(location) }
                            }
                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {}
                            @Deprecated("Legacy callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        }
                        continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                        try {
                            providers.forEach { manager.requestLocationUpdates(it, 0L, 0f, listener, Looper.getMainLooper()) }
                        } catch (e: Exception) { manager.removeUpdates(listener); if (continuation.isActive) continuation.resumeWithException(e) }
                    }
                }
                if (result == null) error = R.string.location_unavailable
                else callback(result.latitude, result.longitude)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = R.string.location_unavailable }
            finally { busy = false }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.any { it }) locate() else error = R.string.map_permission_denied
    }
    SmallFloatingActionButton(modifier = modifier, onClick = {
        if (!busy) {
            val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) locate()
            else permission.launch(permissions)
        }
    }) { Text(stringResource(if (busy) R.string.location_loading else R.string.map_my_location)) }
    error?.let { message ->
        AlertDialog(onDismissRequest = { error = null; settings = false }, text = { Text(stringResource(message)) },
            confirmButton = { TextButton(onClick = {
                if (settings) runCatching { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                error = null; settings = false
            }) { Text(stringResource(if (settings) R.string.location_settings else R.string.photo_ok)) } })
    }
}
