package com.example.servinow.features.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.servinow.R

@Composable
fun WelcomeScreen(onStart: () -> Unit, onLogin: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Spacer(Modifier.height(28.dp))
        Image(painterResource(R.drawable.servinow_logo), "Logo de ServiNow: hogar y rayo", Modifier.size(124.dp))
        Text("ServiNow", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Personas que ayudan.\nTrabajos que conectan.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Encuentra ayuda para lo que necesitas o nuevas oportunidades para lo que sabes hacer.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Un lugar para colaborar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Publica una necesidad, conversa y elige con quién trabajar.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) { Text("Empezar") }
        TextButton(onClick = onLogin) { Text("Ya tengo cuenta · Iniciar sesión") }
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit, onLogin: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    val titles = listOf("Cuéntanos qué necesitas", "Comparte tu talento", "Elige con información")
    val descriptions = listOf(
        "Publica el trabajo, indica dónde se realizará y recibe ofertas de la comunidad.",
        "Explora trabajos, presenta tu propuesta y acuerda los detalles con quien necesita tu ayuda.",
        "Consulta perfiles y reseñas, sigue el trabajo y comparte tu experiencia al finalizar.",
    )
    val icons = listOf(R.drawable.ic_home, R.drawable.ic_work, R.drawable.ic_account)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Conoce ServiNow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onLogin) { Text("Omitir") }
        }
        Text("Paso ${step + 1} de 3", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                Surface(modifier = Modifier.width(42.dp).height(6.dp), color = if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(3.dp)) {}
            }
        }
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(32.dp), modifier = Modifier.padding(vertical = 18.dp).size(152.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(painterResource(icons[step]), null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        Text(titles[step], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(descriptions[step], textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = { if (step < 2) step++ else onFinish() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) { Text(if (step < 2) "Siguiente" else "Crear mi cuenta") }
        if (step > 0) TextButton(onClick = { step-- }) { Text("Anterior") }
        if (step == 2) TextButton(onClick = onLogin) { Text("Ya tengo cuenta") }
    }
}
