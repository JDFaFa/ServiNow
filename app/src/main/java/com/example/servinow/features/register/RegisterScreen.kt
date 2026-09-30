package com.example.servinow.features.register

import com.example.servinow.core.component.screenSpacing
import com.example.servinow.core.component.mapHeight

import com.example.servinow.core.component.PasswordVisibilityButton
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp

@Composable
fun RegisterRoute(viewModel: RegisterViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    BackHandler { if (state.choosingLocation) viewModel.backToForm() else onBack() }
    state.message?.let { message ->
        AlertDialog(onDismissRequest = viewModel::dismissMessage,
            title = { Text("Registro") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text("Entendido") } })
    }
    if (state.choosingLocation) {
        AddressMapScreen(state, viewModel::selectLocation, viewModel::confirmLocation, viewModel::backToForm)
        return
    }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()
            .verticalScroll(rememberScrollState()).padding(screenSpacing()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                TextButton(onClick = onBack) { Text("← Volver al inicio de sesión") }
                Spacer(Modifier.height(16.dp))
                Text("ServiNow.", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                Text("Crea tu cuenta", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Encuentra ayuda o comparte tu talento. Tu próximo trabajo comienza aquí.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(value = state.name, onValueChange = viewModel::nameChanged,
                        label = { Text("Nombre completo") }, singleLine = true,
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
                    OutlinedTextField(value = state.email, onValueChange = viewModel::emailChanged,
                        label = { Text("Correo electrónico") }, singleLine = true,
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                    RegistrationPasswordField("Contraseña", state.password, viewModel::passwordChanged,
                        state.passwordVisible, viewModel::togglePassword)
                    RegistrationPasswordField("Confirmar contraseña", state.confirmation, viewModel::confirmationChanged,
                        state.confirmationVisible, viewModel::toggleConfirmation, viewModel::submit)
                    Button(onClick = viewModel::submit, shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text("Continuar", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = viewModel::google, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("Continuar con Google", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("¿Ya tienes cuenta? Inicia sesión")
                }
            }
        }
    }
}

@Composable
private fun RegistrationPasswordField(label: String, value: String, onChange: (String) -> Unit,
    visible: Boolean, onToggle: () -> Unit, onDone: (() -> Unit)? = null) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { PasswordVisibilityButton(visible, onToggle) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password,
            imeAction = if (onDone == null) ImeAction.Next else ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }))
}
