package com.example.servinow.core.component

import android.Manifest
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.example.servinow.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The UI owns Android launchers; callers receive only a URI after a successful selection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPicker(uri: String?, circular: Boolean = false, onSelected: (String?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sheet by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    fun newPhoto(): File = File.createTempFile("photo_", ".jpg", File(context.filesDir, "photos").apply { mkdirs() })
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val path = pending
        if (success && path != null) onSelected(Uri.fromFile(File(path)).toString())
        else path?.let { File(it).delete() }
        pending = null
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) error = context.getString(R.string.photo_permission)
        else {
            var file: File? = null
            try {
                file = newPhoto()
                pending = file.absolutePath
                camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            } catch (_: Exception) {
                file?.delete(); pending = null; error = context.getString(R.string.photo_failed)
            }
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selected ->
        if (selected != null) scope.launch {
            busy = true
            try {
                val saved = withContext(Dispatchers.IO) {
                    val file = newPhoto()
                    try {
                        val input = context.contentResolver.openInputStream(selected) ?: error("Unreadable image")
                        input.use { source -> file.outputStream().use { destination ->
                            val buffer = ByteArray(8192); var total = 0L
                            while (true) { val count = source.read(buffer); if (count < 0) break
                                total += count; require(total <= 20 * 1024 * 1024) { "Image too large" }
                                destination.write(buffer, 0, count)
                            }
                        } }
                        require(file.length() > 0)
                        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeFile(file.absolutePath, bounds)
                        require(bounds.outWidth > 0 && bounds.outHeight > 0)
                        Uri.fromFile(file).toString()
                    } catch (e: Exception) { file.delete(); throw e }
                }
                onSelected(saved)
            } catch (_: Exception) { error = context.getString(R.string.photo_failed) }
            finally { busy = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (uri != null) AsyncImage(model = uri, contentDescription = stringResource(R.string.photo_preview), contentScale = ContentScale.Crop,
            modifier = if (circular) Modifier.size(112.dp).clip(CircleShape) else Modifier.fillMaxWidth().height(180.dp))
        OutlinedButton(onClick = { sheet = true }, enabled = !busy) { Text(stringResource(R.string.photo_select)) }
        if (uri != null) TextButton(onClick = { onSelected(null) }) { Text(stringResource(R.string.photo_remove)) }
        if (busy) CircularProgressIndicator()
    }
    if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }) {
        Column(Modifier.padding(24.dp)) {
            Text(stringResource(R.string.photo_select), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { sheet = false; permission.launch(Manifest.permission.CAMERA) }) { Text(stringResource(R.string.photo_camera)) }
            TextButton(onClick = {
                sheet = false
                try { gallery.launch("image/*") } catch (_: ActivityNotFoundException) { error = context.getString(R.string.photo_failed) }
            }) { Text(stringResource(R.string.photo_gallery)) }
        }
    }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { error = null }) { Text(stringResource(R.string.photo_ok)) } }) }
}
