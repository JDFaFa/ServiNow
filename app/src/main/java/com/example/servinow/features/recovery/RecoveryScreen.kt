package com.example.servinow.features.recovery

import com.example.servinow.core.component.screenSpacing
import com.example.servinow.core.component.mapHeight

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.servinow.R
import com.example.servinow.core.theme.ServiNowTheme

@Composable
fun RecoveryRoute(viewModel: RecoveryViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val back = { if (state.step == RecoveryStep.EMAIL) onBack() else viewModel.back() }
    BackHandler(onBack = back)
    state.message?.let { message ->
        AlertDialog(onDismissRequest = viewModel::dismissMessage,
            title = { Text("Recuperar contraseña") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text("Entendido") } })
    }
    Scaffold { padding ->
        if (state.step == RecoveryStep.NEW_PASSWORD) {
            NewPasswordScreen(state, viewModel::passwordChanged, viewModel::confirmationChanged,
                viewModel::savePassword, back, viewModel::notifications, Modifier.padding(padding).consumeWindowInsets(padding))
        } else {
            RecoveryStepsScreen(state, viewModel::emailChanged, viewModel::submit, back,
                onBack, viewModel::resend, viewModel::previewLink, viewModel::notifications,
                Modifier.padding(padding).consumeWindowInsets(padding))
        }
    }
}

@Composable
private fun RecoveryStepsScreen(state: RecoveryUiState, onEmail: (String) -> Unit,
    onSubmit: () -> Unit, onBack: () -> Unit, onLogin: () -> Unit,
    onResend: () -> Unit, onPreviewLink: () -> Unit, onNotifications: () -> Unit,
    modifier: Modifier = Modifier) {
    val sent = state.step == RecoveryStep.SENT
    Column(modifier.fillMaxSize().imePadding()) {
        RecoveryHeader(if (sent) "Enlace enviado" else "Recuperar contraseña", onBack, onNotifications)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(screenSpacing()),
            horizontalAlignment = if (sent) Alignment.CenterHorizontally else Alignment.Start) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp),
                modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(if (sent) R.drawable.ic_circle_check else R.drawable.ic_mail),
                        contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(if (sent) "Revisa tu correo." else "Recupera tu acceso.",
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                textAlign = if (sent) TextAlign.Center else TextAlign.Start)
            Spacer(Modifier.height(12.dp))
            Text(if (sent) "Si existe una cuenta con ese correo, recibirás un enlace para restablecer tu contraseña."
                else "Escribe el correo de tu cuenta. Te enviaremos un enlace para crear una nueva contraseña.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (sent) TextAlign.Center else TextAlign.Start)
            Spacer(Modifier.height(24.dp))
            if (!sent) {
                Text("Correo electrónico", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = state.email, onValueChange = onEmail,
                    placeholder = { Text("nombre@correo.com") }, singleLine = true,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }))
                Spacer(Modifier.height(16.dp))
                Button(onClick = onSubmit, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Enviar enlace") }
            }
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                Text(if (sent) "Revisa también la carpeta de spam. En este prototipo no se envían correos."
                    else "Disponible para usuarios y moderadores.", modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(20.dp))
            if (sent) {
                Button(onClick = onLogin, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Volver a iniciar sesión") }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onResend, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Reenviar enlace") }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onPreviewLink, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Vista: abrir enlace recibido") }
            } else {
                TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Volver al inicio de sesión")
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 760)
@Composable
private fun Recovery05Preview() {
    ServiNowTheme { RecoveryStepsScreen(RecoveryUiState(), {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 760)
@Composable
private fun Recovery06Preview() {
    ServiNowTheme { RecoveryStepsScreen(RecoveryUiState(step = RecoveryStep.SENT), {}, {}, {}, {}, {}, {}, {}) }
}
