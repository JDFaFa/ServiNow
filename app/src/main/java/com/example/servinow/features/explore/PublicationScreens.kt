package com.example.servinow.features.explore

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.servinow.core.component.mapHeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val publicationPages = setOf("create", "create-details", "post-location", "preview", "submitted", "mine", "edit", "rejected")
fun publicationTitle(page: String) = when(page) {
    "create" -> "Crear · servicio"; "create-details" -> "Descripción y fotos"; "post-location" -> "Ubicación del trabajo"
    "preview" -> "Revisar y publicar"; "submitted" -> "Publicación pendiente"; "mine" -> "Mis publicaciones"
    "edit" -> "Editar publicación"; "rejected" -> "Publicación rechazada"; else -> "ServiNow."
}
@Composable
fun PublicationContent(page: String, vm: PublicationViewModel, go: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val p = state.draft
    val context = LocalContext.current
    var deleting by remember { mutableStateOf<Publication?>(null) }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val accepted = uris.filter { uri -> runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess }.map { it.toString() }
        if (uris.isNotEmpty() && accepted.isEmpty()) vm.message("No se pudo acceder a las fotos. Selecciónalas de nuevo.")
        vm.change(vm.state.value.draft.copy(photos = (vm.state.value.draft.photos + accepted).distinct()))
    }
    state.message?.let { text -> AlertDialog(onDismissRequest = { vm.message(null) }, title = { Text("Publicación") },
        text = { Text(text) }, confirmButton = { TextButton(onClick = { vm.message(null) }) { Text("Entendido") } }) }
    deleting?.let { post -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("¿Eliminar publicación?") },
        text = { Text(post.title) }, confirmButton = { TextButton(onClick = { vm.delete(post); deleting = null; go("mine") }) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }) }
    when(page) {
        "create" -> {
            Step(1); Title("¿En qué necesitas ayuda?")
            Text("Elige una categoría y dale un nombre a tu trabajo.")
            categories.drop(1).chunked(2).forEach { pair -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { cat -> FilterChip(selected = p.category == cat, onClick = { vm.change(p.copy(category = cat)) }, label = { Text(cat) }, modifier = Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            } }
            Field("Título del trabajo",p.title) { vm.change(p.copy(title = it)) }
            Action("Continuar") { if (vm.validate(1)) go("create-details") }
        }
        "create-details", "edit" -> {
            if (page == "edit") {
                Title("Mejoremos tu publicación.")
                Field("Título",p.title) { vm.change(p.copy(title = it)) }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.drop(1).forEach { cat -> FilterChip(p.category == cat, { vm.change(p.copy(category = cat)) }, label = { Text(cat) }) }
                }
            } else { Step(2); Title("Cuéntanos los detalles.") }
            Field("Descripción",p.description, multiline = true) { vm.change(p.copy(description = it)) }
            Field("Presupuesto estimado (COP)",p.budget, numeric = true) { vm.change(p.copy(budget = it)) }
            Text("El trabajador puede proponer otro valor.")
            Text("Fotos del trabajo · mínimo 1", fontWeight = FontWeight.Bold)
            p.photos.forEach { uri ->
                LocalPhoto(uri)
                TextButton(onClick = { vm.change(p.copy(photos = p.photos - uri)) }) { Text("Quitar foto") }
            }
            OutlinedButton(onClick = { photos.launch(arrayOf("image/jpeg", "image/png")) }, modifier = Modifier.fillMaxWidth()) { Text("Agregar fotos") }
            if (page == "edit") {
                OutlinedButton(onClick = { go("post-location") }) { Text("Cambiar ubicación en mapa") }
                Text("Los cambios dejarán la publicación pendiente de revisión.")
                Action("Guardar y enviar a revisión") { if (vm.save()) go("submitted") }
            } else Action("Continuar · ubicación") { if (vm.validate(2)) go("post-location") }
        }
        "post-location" -> {
            Step(3); Title("¿Dónde es el trabajo?"); Text("Toca el punto exacto en el mapa.")
            ExploreMap(emptyList(), SearchFilters(latitude = p.latitude ?: 4.6486, longitude = p.longitude ?: -74.0628),
                Modifier.fillMaxWidth().height(mapHeight()).clip(RoundedCornerShape(20.dp)), {},
                onLocation = { lat, lng -> vm.change(vm.state.value.draft.copy(latitude = lat, longitude = lng)) })
            Text(if (p.latitude == null) "Selecciona una ubicación." else "✓ Ubicación seleccionada\n${p.latitude}, ${p.longitude}")
            Action(if (state.editing) "Volver a editar" else "Continuar · revisar") {
                if (vm.validate(3)) go(if (state.editing) "edit" else "preview")
            }
        }
        "preview" -> {
            Step(4); Title("Todo listo para revisar.")
            p.photos.forEach { LocalPhoto(it) }
            Text(p.category); Title(p.title); Text(p.description)
            Text("${p.latitude}, ${p.longitude}"); Text("$${p.budget} COP", fontWeight = FontWeight.Bold)
            Text("Primero pasará por moderación. No aparecerá en el feed público mientras esté pendiente.")
            Action("Enviar a revisión") { if (vm.save()) go("submitted") }
            OutlinedButton(onClick = { go("create") }) { Text("Volver a editar") }
        }
        "submitted" -> {
            Title("¡Ya diste el primer paso!"); Text("Pendiente de verificación", color = MaterialTheme.colorScheme.primary)
            Text(p.title, fontWeight = FontWeight.Bold)
            Text("Publicación guardada en este dispositivo. Aún no aparece en el feed público.")
            Text("La revisión por un moderador estará disponible cuando conectemos el servicio de datos.")
            Action("Ver mis publicaciones") { go("mine") }
            OutlinedButton(onClick = { go("home") }) { Text("Ir al inicio") }
        }
        "mine" -> {
            Title("Mis publicaciones"); Text("Acompaña cada trabajo, de principio a fin.")
            Action("Nueva publicación") { vm.start(); go("create") }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todas","Activas","Pendientes","Finalizadas").forEach { filter -> FilterChip(state.filter == filter, { vm.filter(filter) }, label = { Text(filter) }) }
            }
            val visible = state.posts.filter { when(state.filter) { "Pendientes" -> it.status == "Pendiente"; "Finalizadas" -> it.status == "Finalizada"; "Activas" -> it.status == "Verificada"; else -> true } }
            if (visible.isEmpty()) Text("No hay publicaciones en esta sección.")
            visible.forEach { post ->
                Surface(shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp,MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(post.status, color = MaterialTheme.colorScheme.primary); Text(post.title,fontWeight = FontWeight.Bold)
                        Text(post.description, maxLines = 3)
                        if (post.status == "Rechazada") TextButton(onClick = { vm.edit(post); go("rejected") }) { Text("Ver motivo y corregir") }
                        OutlinedButton(onClick = { vm.edit(post); go("edit") }) { Text("Editar") }
                        TextButton(onClick = { deleting = post }) { Text("Eliminar") }
                    }
                }
            }
            TextButton(onClick = { go("rejected") }) { Text("Ver ejemplo de publicación rechazada") }
        }
        "rejected" -> {
            val real = p.status == "Rechazada"
            Text(if (real) "Rechazada" else "Ejemplo de rechazo", color = MaterialTheme.colorScheme.error)
            Title("Podemos mejorarlo."); Text("Una publicación rechazada no se muestra en el feed público.")
            Surface(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp,MaterialTheme.colorScheme.outline)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Motivo del moderador",fontWeight = FontWeight.Bold)
                    Text(if (real) p.reason else "La imagen no permite identificar el trabajo. Agrega una foto clara y precisa qué reparación necesitas.")
                }
            }
            if (!real) Text("Esta vista es un ejemplo; ninguna de tus publicaciones ha sido rechazada.")
            Action(if (real) "Corregir y reenviar" else "Crear publicación corregida") {
                if (!real) vm.start()
                go(if (real) "edit" else "create")
            }
            if (real) OutlinedButton(onClick = { deleting = p }) { Text("Eliminar publicación") }
        }
    }
}
@Composable private fun Step(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(4) { index -> Surface(color = if (index < step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(4.dp), modifier = Modifier.weight(1f).height(5.dp)) {} }
    }
    Text("Paso $step de 4", color = MaterialTheme.colorScheme.primary)
}
@Composable private fun Title(text: String) { Text(text,style = MaterialTheme.typography.headlineSmall,fontWeight = FontWeight.Bold) }
@Composable private fun Action(text: String, click: () -> Unit) { Button(onClick = click, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Text(text) } }
@Composable private fun Field(label: String,value: String,multiline: Boolean = false,numeric: Boolean = false,change: (String) -> Unit) {
    OutlinedTextField(value,change,label = { Text(label) },modifier = Modifier.fillMaxWidth(),shape = RoundedCornerShape(16.dp),
        singleLine = !multiline,minLines = if (multiline) 4 else 1,maxLines = if (multiline) 8 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text))
}
@Composable internal fun LocalPhoto(uri: String) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null,uri) {
        value = withContext(Dispatchers.IO) { runCatching {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it,null,options) }
            options.inSampleSize = 1
            while (options.outWidth / options.inSampleSize > 1200 || options.outHeight / options.inSampleSize > 1200) options.inSampleSize *= 2
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it,null,options) }
        }.getOrNull() }
    }
    bitmap?.let { Image(it.asImageBitmap(),"Foto del trabajo",modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp)),contentScale = ContentScale.Crop) }
        ?: Text("Vista previa de foto no disponible")
}
