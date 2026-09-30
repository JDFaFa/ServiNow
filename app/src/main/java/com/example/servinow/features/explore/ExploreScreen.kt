package com.example.servinow.features.explore

import com.example.servinow.core.component.screenSpacing
import com.example.servinow.core.component.mapHeight

import androidx.activity.compose.BackHandler
import androidx.compose.ui.res.painterResource
import com.example.servinow.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.servinow.features.recovery.RecoveryHeader
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ExploreRoute(vm: ExploreViewModel, publications: PublicationViewModel, onExit: () -> Unit) {
    val state by vm.state.collectAsState()
    val scrollState = rememberScrollState()
    LaunchedEffect(state.page) { scrollState.scrollTo(0) }
    val pending = { vm.message("Esta sección se implementará en la siguiente etapa.") }
    BackHandler { if (state.page == "home") onExit() else vm.back() }
    state.message?.let { message -> AlertDialog(onDismissRequest = { vm.message(null) },
        title = { Text("ServiNow") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { vm.message(null) }) { Text("Entendido") } }) }
    Scaffold(bottomBar = {
        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(27.dp),
            modifier = Modifier.navigationBarsPadding().padding(12.dp).fillMaxWidth()) {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Inicio", "Mis trabajos", "Avisos", "Mi cuenta").forEachIndexed { index, label ->
                    Surface(onClick = { if (index == 0) vm.page("home") else if (index == 1) vm.page("mine") else pending() },
                        color = if ((index == 1 && state.page in publicationPages) || (index == 0 && state.page !in publicationPages)) Color.White else MaterialTheme.colorScheme.primary,
                        contentColor = if ((index == 1 && state.page in publicationPages) || (index == 0 && state.page !in publicationPages)) MaterialTheme.colorScheme.primary else Color.White,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .4f)),
                        shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(painterResource(listOf(R.drawable.ic_home, R.drawable.ic_work, R.drawable.ic_bell, R.drawable.ic_account)[index]), null, modifier = Modifier.size(24.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            RecoveryHeader(if (state.page in publicationPages) publicationTitle(state.page) else when(state.page) { "comments" -> "Comentarios"; "detail" -> "Detalle del trabajo"; "filters" -> "Filtros de búsqueda"; "map" -> "Feed en mapa"; "location" -> "Elegir ubicación"; else -> "ServiNow." },
                { if (state.page == "home") onExit() else vm.back() }, pending)
            Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(screenSpacing()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when(state.page) {
                    in publicationPages -> PublicationContent(state.page, publications, vm::page)
                    "comments" -> {
                        Heading("Una conversación útil.")
                        Text(state.jobs.find { it.id == state.selectedId }?.title.orEmpty())
                        state.comments[state.selectedId].orEmpty().forEach { text ->
                            Surface(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp)) { Text("Tú", fontWeight = FontWeight.Bold); Text(text) }
                            }
                        }
                        OutlinedTextField(state.commentDraft, vm::comment, label = { Text("Agregar un comentario") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6)
                        Primary("Publicar comentario", vm::postComment)
                        Text("Comentarios guardados en este dispositivo. Las notificaciones al autor requieren conectar el backend.", style = MaterialTheme.typography.bodySmall)
                    }

                    "detail" -> {
                        val job = state.jobs.find { it.id == state.selectedId }
                        if (job != null) {
                            Text("${job.category} · Verificada", color = MaterialTheme.colorScheme.primary)
                            Heading(job.title)
                            Text(job.area)
                            Text("Presupuesto: $${NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(job.price)} COP", fontWeight = FontWeight.Bold)
                            Text(when(job.id) {
                                1 -> "Hay una fuga debajo del lavaplatos. Necesito revisar el sifón y reparar la conexión para que deje de perder agua."
                                2 -> "Necesito instalar dos lámparas de techo y revisar que sus conexiones queden seguras."
                                else -> "Las puertas del armario rozan al cerrar. Necesito ajustar las bisagras y revisar su alineación."
                            })
                            Text("Ubicación del trabajo", fontWeight = FontWeight.Bold)
                            ExploreMap(listOf(job), state.filters.copy(latitude = job.latitude, longitude = job.longitude),
                                Modifier.fillMaxWidth().height(mapHeight()).clip(RoundedCornerShape(20.dp)), {})
                            Text("${job.latitude}, ${job.longitude}", style = MaterialTheme.typography.bodySmall)
                            Primary(if (job.id in state.interested) "✓ Te interesa · ${job.votes}" else "Me interesa · ${job.votes}", vm::toggleInterest)
                            OutlinedButton(onClick = { vm.page("comments") }) { Text("Abrir conversación") }
                            Heading("Comentarios")
                            val comments = state.comments[job.id].orEmpty()
                            if (comments.isEmpty()) Text("Sé la primera persona en aportar información.")
                            comments.forEach { comment ->
                                Surface(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) { Text("Tú", fontWeight = FontWeight.Bold); Text(comment) }
                                }
                            }
                            OutlinedTextField(state.commentDraft, vm::comment, label = { Text("Agregar un comentario") },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), minLines = 2, maxLines = 5)
                            Primary("Publicar comentario", vm::postComment)
                            Text("Modo local: tus comentarios e intereses se guardan en este dispositivo. Aún no se comparten con otros usuarios.", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    "filters" -> {
                        Heading("Encuentra lo que buscas.")
                        Text("Ajusta los resultados a tu zona y especialidad.")
                        Choice("Categoría", state.draft.category, categories) { vm.draft(state.draft.copy(category = it)) }
                        Choice("Ubicación", state.draft.area, listOf("Cerca de mí · hasta 5 km", "En mi ciudad · Bogotá", "Otra ubicación en el mapa")) { vm.draft(state.draft.copy(area = it)) }
                        OutlinedButton(onClick = { vm.page("location") }, modifier = Modifier.fillMaxWidth()) { Text("Elegir ubicación en mapa") }
                        Choice("Ordenar por", state.draft.order, listOf("Más recientes", "Más relevantes · más votos", "Menor distancia")) { vm.draft(state.draft.copy(order = it)) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(state.draft.openOnly, { vm.draft(state.draft.copy(openOnly = it)) })
                            Text("Solo trabajos abiertos")
                        }
                        Primary("Aplicar filtros", vm::apply)
                        OutlinedButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) { Text("Restablecer filtros") }
                    }
                    "location" -> {
                        Heading("Elige tu zona")
                        Text("Toca el mapa para definir el centro de búsqueda. Se mostrarán trabajos a un máximo de 5 km.")
                        ExploreMap(emptyList(), state.draft, Modifier.fillMaxWidth().height(mapHeight()).clip(RoundedCornerShape(20.dp)), {},
                            onLocation = { lat, lng -> vm.draft(state.draft.copy(latitude = lat, longitude = lng, area = "Otra ubicación en el mapa")) })
                        Text(String.format(Locale.US, "%.5f, %.5f", state.draft.latitude, state.draft.longitude))
                        Primary("Usar esta ubicación") { vm.page("filters") }
                    }
                    "map" -> {
                        Text("EXPLORAR TU ZONA", color = MaterialTheme.colorScheme.primary)
                        Heading("El talento empieza cerca.")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (state.filters.area == "En mi ciudad · Bogotá") "Bogotá" else "Zona elegida · hasta 5 km", modifier = Modifier.weight(1f))
                            TextButton(onClick = vm::editFilters) { Text("Filtros") }
                        }
                        ExploreMap(state.results, state.filters, Modifier.fillMaxWidth().height(mapHeight()).clip(RoundedCornerShape(20.dp)), vm::select)
                        Text("${state.results.size} trabajos · selecciona un marcador", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { vm.page("home") }) { Text("Ver lista") }
                        val selected = state.results.find { it.id == state.selectedId } ?: state.results.firstOrNull()
                        if (selected == null) Text("No hay trabajos con estos filtros.") else JobCard(selected, state.filters) { vm.openJob(selected.id) }
                    }
                    else -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text("Tu comunidad en", style = MaterialTheme.typography.bodySmall); Text("Chapinero, Bogotá", fontWeight = FontWeight.Bold) }
                            TextButton(onClick = vm::editFilters) { Text("Filtros") }
                        }
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Hola, Valentina", color = MaterialTheme.colorScheme.primary)
                                Heading("¿Qué resolvemos hoy?")
                                Text("Publica lo que necesitas y recibe ofertas de personas con talento.")
                                Primary("Crear una publicación +") { publications.start(); vm.page("create") }
                            }
                        }
                        Text("Vista de demostración · publicaciones de ejemplo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(state.query, vm::query, placeholder = { Text("Buscar un trabajo o servicio…") },
                            singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.take(4).forEach { category -> FilterChip(state.filters.category == category, { vm.category(category) }, label = { Text(category) }) }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Trabajos cerca de ti", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.page("map") }) { Text("Ver mapa") }
                        }
                        if (state.results.isEmpty()) Text("No encontramos trabajos. Prueba otra categoría o restablece los filtros.")
                        state.results.forEach { job -> JobCard(job, state.filters) { vm.openJob(job.id) } }
                    }
                }
            }
        }
    }
}
@Composable private fun Heading(text: String) { Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
@Composable private fun Primary(text: String, onClick: () -> Unit) { Button(onClick, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Text(text, fontWeight = FontWeight.Bold) } }
@Composable private fun Choice(label: String, value: String, choices: List<String>, onChoice: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Text("$value ▾") }
            DropdownMenu(expanded, { expanded = false }) { choices.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onChoice(option); expanded = false }) } }
        }
    }
}
@Composable private fun JobCard(job: Job, filters: SearchFilters, onOpen: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${job.category}  ·  Verificada", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            Text(job.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${job.area} · ${String.format(Locale.US, "%.1f", distanceKm(job, filters))} km", style = MaterialTheme.typography.bodySmall)
            Text("$${NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(job.price)} COP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Hace ${job.minutes} min", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .3f))
            Text("${job.votes} personas interesadas", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onOpen) { Text("Ver trabajo →") }
        }
    }
}
