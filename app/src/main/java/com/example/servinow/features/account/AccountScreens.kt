package com.example.servinow.features.account
import com.example.servinow.domain.model.SearchFilters
import com.example.servinow.domain.model.categories
import com.example.servinow.domain.validation.professionalError
import com.example.servinow.domain.validation.reputationLevel

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.servinow.features.explore.*
import com.example.servinow.features.services.*

val accountPages=setOf("cv","professional","notifications","dashboard","achievements","account","edit-account","account-map","notice-detail","delete-account","mod-login","moderation")
fun accountTitle(page:String)=when(page){"notice-detail"->"Mi publicación";"cv"->"Currículum";"professional"->"Perfil profesional";"notifications"->"Avisos";"dashboard"->"Mis estadísticas";"achievements"->"Reputación y logros";"edit-account"->"Datos personales";"account-map"->"Mi dirección";"delete-account"->"Eliminar cuenta";"mod-login"->"Acceso de moderador";"moderation"->"Bandeja de moderación";else->"Mi cuenta"}
@Composable private fun Card(content:@Composable ColumnScope.()->Unit){Surface(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)}}
@Composable private fun Field(label:String,value:String,change:(String)->Unit,multiline:Boolean=false){OutlinedTextField(value,change,label={Text(label)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),minLines=if(multiline)3 else 1,singleLine=!multiline)}
@Composable private fun Action(text:String,click:()->Unit){Button(onClick=click,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Text(text)}}
@Composable
fun AccountContent(page:String,vm:AccountViewModel,publications:PublicationViewModel,services:ServiceViewModel,comments:Int,go:(String)->Unit,recover:()->Unit,exit:()->Unit,delete:()->Unit){
 val a by vm.data.collectAsState();val notices by vm.notices.collectAsState();val points by vm.points.collectAsState();val ps by publications.state.collectAsState();val ss by services.state.collectAsState()
 val professionals=services.professionals
 val context=LocalContext.current
 var draft by remember(a){mutableStateOf(a)}
 var message by rememberSaveable { mutableStateOf<String?>(null) }
 // The personal draft survives navigation to the embedded map because it is saved before opening it.
 val file=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}.onSuccess{draft=draft.copy(cv=uri.toString())}.onFailure{message="No se pudo conservar el acceso al PDF."}}}
 val photo=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}.onSuccess{draft=draft.copy(photo=uri.toString())}.onFailure{message="No se pudo acceder a la imagen."}}}
 message?.let { AlertDialog(onDismissRequest={message=null},title={Text("ServiNow")},text={Text(it)},confirmButton={TextButton(onClick={message=null}){Text("Entendido")}}) }
 when(page){
 "notice-detail"->{
  val post=ps.posts.find{it.id==ps.draft.id}
  if(post==null)Text("Esta publicación ya no está disponible.") else Card{
   Text(post.title,style=MaterialTheme.typography.headlineSmall)
   Text("${post.category} · ${post.status}");Text(post.description)
   Text("${post.budget} COP · ${post.latitude}, ${post.longitude}")
   post.photos.forEach{LocalPhoto(it)}
   if(post.reason.isNotBlank())Text("Motivo: ${post.reason}")
  }
 }

 "cv"->{
  val person=professionals.firstOrNull{it.id==ss.profile}?:professionals.first()
  Text(person.name,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
  Text("Currículum de ejemplo · ${person.trade}")
  Card{Text("Acerca de mí",fontWeight=FontWeight.Bold);Text("${person.years} años de experiencia en ${person.trade.lowercase()}. Atención cuidadosa y presupuestos claros.")}
  Card{Text("Experiencia",fontWeight=FontWeight.Bold);Text("Profesional independiente · servicios a domicilio");Text("Formación y habilidades");Text("Mantenimiento, diagnóstico, atención al cliente y elaboración de presupuestos.")}
  Text("Este perfil pertenece al catálogo de demostración; no es tu perfil editable.",style=MaterialTheme.typography.bodySmall)
  Action("Volver al perfil"){go("profile")}
 }
 "professional"->{
  Text("Tu experiencia abre puertas.",style=MaterialTheme.typography.headlineMedium)
  Field("Título profesional",draft.title,{draft=draft.copy(title=it)})
  Field("Acerca de ti",draft.about,{draft=draft.copy(about=it)},true)
  Field("Años de experiencia",draft.years,{draft=draft.copy(years=it)})
  var expanded by remember { mutableStateOf(false) }
  Box{OutlinedButton(onClick={expanded=true}){Text("Especialidad: ${draft.specialty}")};DropdownMenu(expanded,{expanded=false}){categories.drop(1).forEach{cat->DropdownMenuItem(text={Text(cat)},onClick={draft=draft.copy(specialty=cat);expanded=false})}}}
  OutlinedButton(onClick={file.launch(arrayOf("application/pdf"))}){Text(if(draft.cv.isEmpty())"Adjuntar CV en PDF" else "Reemplazar CV en PDF")}
  if(draft.cv.isNotEmpty())OutlinedButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(draft.cv),"application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}.onFailure{message="Instala un lector de PDF para abrir tu currículum."}}){Text("Ver mi PDF")}
  Action("Guardar perfil profesional"){val error=professionalError(draft);message=error?:if(vm.save(draft))"Perfil guardado en este dispositivo." else "No se pudo guardar."}
 }
 "notifications"->{
  Text("${notices.count{!it.read}} avisos sin leer",style=MaterialTheme.typography.headlineSmall)
  Text("Eventos locales de tu actividad. Los avisos de otros usuarios requieren conexión al servidor.",style=MaterialTheme.typography.bodySmall)
  TextButton(onClick=vm::markAll){Text("Marcar todos como leídos")}
  if(notices.isEmpty())Text("Todavía no tienes avisos.")
  notices.forEach{n->Card{Text(if(n.read)"Leído" else "Nuevo",color=MaterialTheme.colorScheme.primary);Text(n.text);TextButton(onClick={vm.mark(n.id,!n.read)}){Text(if(n.read)"Marcar no leído" else "Marcar leído")};Action("Ver detalle"){vm.mark(n.id,true);if(n.post.isNotEmpty()){val post=ps.posts.find{it.id==n.post};if(post==null)message="Esta publicación fue eliminada." else {publications.edit(post);go("notice-detail")}}else go(n.target)}}}
 }
 "dashboard"->{
  Text("Tu actividad en ServiNow",style=MaterialTheme.typography.headlineMedium)
  listOf("Activas" to ps.posts.count{it.status in listOf("Verificada","En progreso")},"Finalizadas" to ps.posts.count{it.status in listOf("Finalizada","Completada")},"Pendientes de verificación" to ps.posts.count{it.status=="Pendiente"},"Rechazadas" to ps.posts.count{it.status=="Rechazada"}).forEach{(label,count)->Card{Text(count.toString(),style=MaterialTheme.typography.headlineMedium);Text(label)}}
  Card{Text("$points puntos · ${reputationLevel(points)}");Text("$comments comentarios realizados");Text("Votos recibidos: no disponibles sin servidor")}
  Action("Ver mis logros"){go("achievements")}
 }
 "achievements"->{
  Text("Cada aporte cuenta.",style=MaterialTheme.typography.headlineMedium)
  Card{Text("${reputationLevel(points)} · $points puntos");val next=when{points<100->100;points<500->500;points<1500->1500;else->points};Text(if(points<next)"Faltan ${next-points} puntos para el siguiente nivel." else "Alcanzaste el nivel máximo.")}
  Text("Vecino: 0–99 · Colaborador: 100–499\nExperto: 500–1499 · Referente: 1500+")
  listOf("badge-first" to "Primera publicación","badge-comments" to "10 comentarios","badge-verified" to "10 publicaciones verificadas").forEach{(key,title)->Card{Text(title,fontWeight=FontWeight.Bold);Text(if(vm.achieved(key))"Desbloqueada ✓" else "Por conseguir")}}
  Card{Text("Publicación destacada del mes");Text("Disponible cuando se conecten los votos de la comunidad.")}
  Text("Crear: +10 · Verificación: +20 · Comentar: +2. Cada logro se registra una sola vez. Los votos propios no otorgan puntos.")
 }
 "account"->{
  Card{Text(a.name.ifBlank{"Tu cuenta local"},style=MaterialTheme.typography.headlineSmall);Text(a.email.ifBlank{"Completa tus datos personales"});Text("${reputationLevel(points)} · $points puntos")}
  Row{Switch(a.worker,{if(!vm.save(a.copy(worker=it)))message="No se pudo guardar."});Text(if(a.worker)"Ofrezco servicios" else "Necesito un servicio",modifier=Modifier.padding(12.dp))}
  listOf("Datos personales" to "edit-account","Perfil profesional y CV" to "professional","Estadísticas" to "dashboard","Logros" to "achievements","Notificaciones" to "notifications").forEach{(label,target)->OutlinedButton(onClick={go(target)},modifier=Modifier.fillMaxWidth()){Text(label)}}
  TextButton(onClick=recover){Text("Recuperar contraseña")}
  TextButton(onClick={go("mod-login")}){Text("Acceso de moderador")}
  Action("Salir al inicio de sesión",exit)
  TextButton(onClick={go("delete-account")}){Text("Eliminar datos de mi cuenta",color=MaterialTheme.colorScheme.error)}
 }
 "edit-account"->{
  if(draft.photo.isNotEmpty())LocalPhoto(draft.photo)
  OutlinedButton(onClick={photo.launch(arrayOf("image/jpeg","image/png"))}){Text("Elegir foto de perfil")}
  Field("Nombre completo",draft.name,{draft=draft.copy(name=it)})
  Field("Correo electrónico",draft.email,{draft=draft.copy(email=it)})
  Field("Teléfono (opcional)",draft.phone,{draft=draft.copy(phone=it)})
  Text(if(draft.lat==null)"Dirección sin seleccionar" else "${draft.lat}, ${draft.lng}")
  Action("Elegir dirección en mapa"){if(vm.save(draft))go("account-map")else message="No se pudo guardar."}
  Action("Guardar datos"){message=when{draft.name.isBlank()->"Escribe tu nombre.";com.example.servinow.core.util.EmailValidator.error(draft.email)!=null->com.example.servinow.core.util.EmailValidator.error(draft.email);draft.lat==null->"Selecciona tu dirección en el mapa.";vm.save(draft)->"Datos guardados localmente.";else->"No se pudo guardar."}}
 }
 "account-map"->{
  var lat by rememberSaveable{mutableStateOf(a.lat?:4.6486)};var lng by rememberSaveable{mutableStateOf(a.lng?:-74.0628)};var picked by rememberSaveable{mutableStateOf(a.lat!=null)}
  Text("Toca el mapa para seleccionar tu dirección.")
  ExploreMap(emptyList(),SearchFilters(latitude=lat,longitude=lng),Modifier.fillMaxWidth().height(340.dp),{},onLocation={x,y->lat=x;lng=y;picked=true})
  Text("$lat, $lng")
  Action("Usar esta dirección"){if(!picked)message="Selecciona un punto en el mapa." else if(vm.save(a.copy(lat=lat,lng=lng)))go("edit-account") else message="No se pudo guardar."}
 }
 "delete-account"->{
  Text("¿Eliminar tus datos?",style=MaterialTheme.typography.headlineMedium)
  Text("Se borrarán el perfil, publicaciones, ofertas, comentarios, avisos y logros guardados en este dispositivo. No existe todavía una cuenta en el servidor. Tus archivos originales no se eliminan.")
  var confirmation by rememberSaveable{mutableStateOf("")}
  Field("Escribe ELIMINAR",confirmation,{confirmation=it})
  Button(onClick={if(confirmation=="ELIMINAR")delete()else message="Escribe ELIMINAR para confirmar."},colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error),modifier=Modifier.fillMaxWidth()){Text("Eliminar datos locales")}
  OutlinedButton(onClick={go("account")}){Text("Conservar mi cuenta")}
 }
 "mod-login"->{
  Text("Acceso de moderador",style=MaterialTheme.typography.headlineMedium)
  var email by rememberSaveable{mutableStateOf("")};var password by rememberSaveable{mutableStateOf("")};var remember by rememberSaveable{mutableStateOf(false)}
  Field("Correo",email,{email=it})
  OutlinedTextField(password,{password=it},label={Text("Contraseña")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth(),singleLine=true)
  Row{Checkbox(remember,{remember=it});Text("Recordarme",modifier=Modifier.padding(top=12.dp))}
  Action("Iniciar sesión"){message="La autenticación de moderadores requiere conectar las cuentas precargadas del servidor. No se han validado estas credenciales."}
  TextButton(onClick=recover){Text("Olvidé mi contraseña")}
  Text("Demostración local: la siguiente bandeja no concede permisos de moderador ni publica cambios.")
  OutlinedButton(onClick={go("moderation")}){Text("Probar bandeja local")}
 }
 "moderation"->{
  Text("Bandeja local · demostración",style=MaterialTheme.typography.headlineSmall)
  Text("${ps.posts.count{it.status=="Pendiente"}} pendientes · ${ps.posts.count{it.status=="Verificada"}} verificadas")
  var filter by rememberSaveable{mutableStateOf("Pendiente")};var selected by rememberSaveable{mutableStateOf<String?>(null)}
  listOf("Pendiente","Verificada","Rechazada").forEach{value->FilterChip(selected=filter==value,onClick={filter=value},label={Text(value)})}
  val posts=ps.posts.filter{it.status==filter};if(posts.isEmpty())Text("No hay publicaciones en esta categoría.")
  posts.forEach{post->Card{Text(post.title,fontWeight=FontWeight.Bold);Text("${post.category} · ${post.status}");TextButton(onClick={selected=if(selected==post.id)null else post.id}){Text(if(selected==post.id)"Ocultar detalle" else "Inspeccionar publicación")};if(selected==post.id){Text(post.description);Text("Presupuesto: ${post.budget} COP");Text("${post.latitude}, ${post.longitude}");post.photos.forEach{LocalPhoto(it)};if(post.reason.isNotEmpty())Text("Motivo: ${post.reason}")}}}
 }
 }
}
