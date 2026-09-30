package com.example.servinow.features.login

import com.example.servinow.core.component.screenSpacing
import com.example.servinow.core.component.mapHeight

import com.example.servinow.core.component.PasswordVisibilityButton
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.servinow.core.theme.ServiNowTheme

@Composable
fun LoginRoute(viewModel: LoginViewModel, onRegister: () -> Unit, onRecover: () -> Unit, onExplore: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            title = { Text("Inicio de sesión") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissMessage) { Text("Entendido") }
            },
        )
    }
    Scaffold { padding ->
        LoginScreen(
            state = state,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onTogglePassword = viewModel::togglePassword,
            onLogin = viewModel::login,
            onGoogleLogin = viewModel::loginWithGoogle,
            onRecover = onRecover,
            onRegister = onRegister,
            onExplore = onExplore,
            onRememberMeChange = viewModel::rememberMeChanged,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        )
    }
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onLogin: () -> Unit,
    onGoogleLogin: () -> Unit,
    onRecover: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
    onRememberMeChange: (Boolean) -> Unit = {},
    onExplore: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = screenSpacing(), vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
            Text("QUÉ BUENO VERTE", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.3.sp))
            Spacer(Modifier.height(12.dp))
            Text("Bienvenido de nuevo.", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Tus trabajos, tus conexiones y nuevas oportunidades te esperan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(28.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Correo electrónico", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = state.email, onValueChange = onEmailChange,
                        placeholder = { Text("nombre@correo.com", fontWeight = FontWeight.SemiBold) },
                        singleLine = true, isError = state.emailError != null,
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Contraseña", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = state.password, onValueChange = onPasswordChange,
                        placeholder = { Text("Tu contraseña", fontWeight = FontWeight.SemiBold) }, singleLine = true,
                        isError = state.passwordError != null,
                        visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { PasswordVisibilityButton(state.passwordVisible, onTogglePassword) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onLogin() }),
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.weight(1f).toggleable(value = state.rememberMe,
                            role = Role.Checkbox, onValueChange = onRememberMeChange),
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.rememberMe, onCheckedChange = null)
                            Text("Recordarme", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = onRecover) {
                            Text("Olvidé mi contraseña", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Button(onClick = onLogin, shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text("Iniciar sesión", fontWeight = FontWeight.Bold)
                    }
            }
            Spacer(Modifier.height(20.dp))
            Text("O continúa con", modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onGoogleLogin,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Continuar con Google", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("¿Eres nuevo? Crea tu cuenta")
            }
            TextButton(onClick = onExplore, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Explorar vistas de demostración")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginPreview() {
    ServiNowTheme { LoginScreen(LoginUiState(), {}, {}, {}, {}, {}, {}, {}) }
}
