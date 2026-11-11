package com.example.servinow.features.cloud

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.servinow.R

@Composable
fun CloudTools(description: String, image: String?, onDescription: (String) -> Unit, onImage: (String) -> Unit,
    showAi: Boolean = true, vm: CloudToolsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmAi by remember { mutableStateOf(false) }
    var confirmImage by remember { mutableStateOf(false) }
    Column {
        if (showAi) OutlinedButton(onClick = { confirmAi = true }, enabled = !state.busy && description.trim().length >= 10, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ai_improve)) }
        if (image != null && !image.startsWith("https://")) OutlinedButton(onClick = { confirmImage = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cloud_upload)) }
        if (image?.startsWith("https://") == true) Text(stringResource(R.string.cloud_uploaded))
        if (showAi && description.trim().length < 10) Text(stringResource(R.string.ai_input_hint), style = MaterialTheme.typography.bodySmall)
        if (state.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(stringResource(R.string.cloud_processing), style = MaterialTheme.typography.bodySmall)
        }
        state.error?.let { code ->
            Text(stringResource(when(code) {
                "SERVICE_NOT_CONFIGURED", "AI_NOT_CONFIGURED", "IMAGES_NOT_CONFIGURED" -> R.string.cloud_not_configured
                "SIGN_IN_REQUIRED", "INVALID_SESSION" -> R.string.cloud_signin_required
                "AI_NEEDS_DETAILS", "INVALID_DESCRIPTION" -> R.string.ai_needs_details
                "AI_INVALID_RESPONSE", "AI_UNAVAILABLE" -> R.string.ai_invalid_response
                "TRY_LATER" -> R.string.ai_rate_limit
                "SERVICE_TIMEOUT" -> R.string.ai_timeout
                "IMAGE_SIZE" -> R.string.cloud_image_size
                "REMOVE_CONTACT_DETAILS" -> R.string.ai_remove_contacts
                else -> R.string.cloud_error
            }), color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirmAi) AlertDialog(onDismissRequest = { confirmAi = false }, title = { Text(stringResource(R.string.ai_improve)) },
        text = { Text(stringResource(R.string.ai_consent)) },
        confirmButton = { TextButton(onClick = { confirmAi = false; vm.improve(description, if (context.resources.configuration.locales[0].language == "es") "es" else "en") }) { Text(stringResource(R.string.chat_send)) } },
        dismissButton = { TextButton(onClick = { confirmAi = false }) { Text(stringResource(R.string.cloud_cancel)) } })
    if (confirmImage) AlertDialog(onDismissRequest = { confirmImage = false }, title = { Text(stringResource(R.string.cloud_upload)) },
        text = { Text(stringResource(R.string.cloud_image_consent)) },
        confirmButton = { TextButton(onClick = { confirmImage = false; image?.let(vm::upload) }) { Text(stringResource(R.string.cloud_upload)) } },
        dismissButton = { TextButton(onClick = { confirmImage = false }) { Text(stringResource(R.string.cloud_cancel)) } })
    state.suggestion?.let { suggestion -> AlertDialog(onDismissRequest = vm::clear,
        title = { Text(stringResource(R.string.ai_review)) }, text = { Text(suggestion, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { onDescription(suggestion); vm.clear() }) { Text(stringResource(R.string.ai_use)) } },
        dismissButton = { TextButton(onClick = vm::clear) { Text(stringResource(R.string.cloud_cancel)) } }) }
    LaunchedEffect(state.imageUrl) { state.imageUrl?.let { onImage(it); vm.clear() } }
}
