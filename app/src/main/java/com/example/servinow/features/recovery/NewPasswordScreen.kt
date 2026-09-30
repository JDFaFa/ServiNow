package com.example.servinow.features.recovery

import com.example.servinow.core.component.screenSpacing
import com.example.servinow.core.component.mapHeight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.servinow.R
import com.example.servinow.core.theme.ServiNowTheme

@Composable
fun NewPasswordScreen(state: RecoveryUiState, onPassword: (String) -> Unit,
    onConfirmation: (String) -> Unit, onSave: () -> Unit, onBack: () -> Unit,
    onNotifications: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().imePadding()) {
        RecoveryHeader("Nueva contraseña", onBack, onNotifications)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(screenSpacing())) {
            Text("Recuperación de cuenta", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("Una contraseña\nnueva.", style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold, lineHeight = 40.sp))
            Spacer(Modifier.height(16.dp))
            Text("Elige una contraseña que puedas recordar y que sea difícil de adivinar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Text("Nueva contraseña", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            PasswordInput(state.password, onPassword, "Mínimo 8 caracteres")
            Spacer(Modifier.height(12.dp))
            Text("Confirmar contraseña", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            PasswordInput(state.confirmation, onConfirmation, "Repite la contraseña", onSave)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onSave, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Guardar contraseña", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PasswordInput(value: String, onChange: (String) -> Unit, placeholder: String, onDone: (() -> Unit)? = null) {
    OutlinedTextField(value = value, onValueChange = onChange, singleLine = true,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password,
            imeAction = if (onDone == null) ImeAction.Next else ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }))
}

@Preview(showBackground = true, widthDp = 390, heightDp = 760)
@Composable
private fun NewPasswordPreview() {
    ServiNowTheme { NewPasswordScreen(RecoveryUiState(), {}, {}, {}, {}, {}) }
}

@Composable
internal fun RecoveryHeader(title: String, onBack: () -> Unit, onNotifications: () -> Unit) {
        Row(Modifier.fillMaxWidth().padding(horizontal = screenSpacing(), vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onBack, shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE1EAF5)), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_back), "Volver", modifier = Modifier.size(20.dp))
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Surface(onClick = onNotifications, shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE1EAF5)), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_bell), "Avisos", modifier = Modifier.size(20.dp))
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(6.dp)
                        .background(Color(0xFFA65B11), CircleShape))
                }
            }
        }
        HorizontalDivider(color = Color(0xFFE1EAF5))
}
