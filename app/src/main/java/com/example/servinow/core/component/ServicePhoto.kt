package com.example.servinow.core.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.servinow.R

/** Local previews and persisted HTTPS images share loading, error and retry feedback. */
@Composable
fun ServicePhoto(model: String?, contentDescription: String?, modifier: Modifier = Modifier,
                 contentScale: ContentScale = ContentScale.Crop) {
    var attempt by remember(model) { mutableIntStateOf(0) }
    var loading by remember(model, attempt) { mutableStateOf(true) }
    var failed by remember(model, attempt) { mutableStateOf(false) }
    Box(modifier, contentAlignment = Alignment.Center) {
        key(model, attempt) {
            AsyncImage(model = model, contentDescription = contentDescription ?: stringResource(R.string.photo_preview),
                contentScale = contentScale,
                modifier = Modifier.matchParentSize().testTag(if (!loading && !failed) "service-photo-loaded" else "service-photo-pending"),
                onLoading = { loading = true; failed = false },
                onSuccess = { loading = false; failed = false },
                onError = { loading = false; failed = true })
        }
        if (loading) CircularProgressIndicator(Modifier.size(24.dp))
        if (failed) Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.photo_load_failed), style = MaterialTheme.typography.labelSmall)
            TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
        }
    }
}
